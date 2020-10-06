/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.builder.LightClassData
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtScript

class KtUltraLightClassForScript(
    script: KtScript,
    private val support: KtUltraLightSupport,
) : KtLightClassForScript(script) {

    override val clsDelegate: PsiClass get() = invalidAccess()

    override val lightClassData: LightClassData get() = invalidAccess()

    override fun getLightClassDataHolder(): LightClassDataHolder.ForScript = invalidAccess()

    override val javaFileStub: PsiJavaFileStub? = null

    private val membersBuilder by lazyPub {
        UltraLightMembersCreator(
            containingClass = this,
            containingClassIsNamedObject = false,
            containingClassIsSealed = true,
            mangleInternalFunctions = true,
            support = support,
        )
    }

    internal inner class KtUltraLightScriptMainParameter(mainMethod: KtUltraLightMethod) :
        KtUltraLightParameter(
            name = "args",
            kotlinOrigin = null,
            support = support,
            ultraLightMethod = mainMethod
        ) {

        override fun getType(): PsiType =
            PsiType.getJavaLangString(manager, resolveScope).createArrayType()

        override fun isVarArgs(): Boolean = false

        override val qualifiedNameForNullabilityAnnotation: String? = null
    }

    private fun MutableList<KtLightMethod>.addScriptDefaultMethods() {

        val defaultConstructorDelegate = LightMethodBuilder(manager, language, name)
            .setConstructor(true)
            .addModifier(PsiModifier.PUBLIC)

        val defaultConstructor = KtUltraLightMethodForSourceDeclaration(
            delegate = defaultConstructorDelegate,
            declaration = script,
            support = support,
            containingClass = this@KtUltraLightClassForScript,
            methodIndex = METHOD_INDEX_FOR_DEFAULT_CTOR,
        )
        defaultConstructorDelegate.addParameter(KtUltraLightScriptMainParameter(defaultConstructor))
        add(defaultConstructor)

        val methodBuilder = LightMethodBuilder(manager, language, "main").apply {
            isConstructor = false
            addModifiers(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)
            setMethodReturnType(PsiType.VOID)
        }

        val mainMethod = KtUltraLightMethodForSourceDeclaration(
            delegate = methodBuilder,
            declaration = script,
            support = support,
            containingClass = this@KtUltraLightClassForScript,
            methodIndex = METHOD_INDEX_FOR_SCRIPT_MAIN,
        )

        methodBuilder.addParameter(KtUltraLightScriptMainParameter(mainMethod))

        add(mainMethod)
    }

    private fun ownMethods(): List<KtLightMethod> {
        val result = mutableListOf<KtLightMethod>()

        result.addScriptDefaultMethods()

        for (declaration in script.declarations.filterNot { it.isHiddenByDeprecation(support) }) {
            when (declaration) {
                is KtNamedFunction -> result.addAll(membersBuilder.createMethods(declaration, forceStatic = false))
                is KtProperty -> result.addAll(
                    membersBuilder.propertyAccessors(declaration, declaration.isVar, forceStatic = false, onlyJvmStatic = false),
                )
            }
        }
        return result
    }

    private val _ownMethods: CachedValue<List<KtLightMethod>> = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result.create(
                ownMethods(),
                KotlinModificationTrackerService.getInstance(project).outOfBlockModificationTracker,
            )
        },
        false,
    )

    override fun getOwnMethods(): List<KtLightMethod> = _ownMethods.value

    private val _ownFields: List<KtLightField> by lazyPub {

        val result = arrayListOf<KtLightField>()
        val usedNames = hashSetOf<String>()

        for (property in script.declarations.filterIsInstance<KtProperty>()) {
            membersBuilder
                .createPropertyField(property, usedNames, forceStatic = false)
                ?.let(result::add)
        }
        result
    }

    override fun getOwnFields(): List<KtLightField> = _ownFields

    override fun copy(): KtLightClassForScript = KtUltraLightClassForScript(script, support)
}