/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiSymbolPointerCreator
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForScriptDefaultConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForScriptMain
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.psi.KtScript

internal class SymbolLightClassForScript private constructor(
    override val script: KtScript,
    private val symbolPointer: KaSymbolPointer<KaScriptSymbol>,
    ktModule: KaModule,
) : KtLightClassForScript, SymbolLightClassBase(ktModule, script.manager) {
    @OptIn(KaImplementationDetail::class)
    internal constructor(
        script: KtScript,
        ktModule: KaModule,
    ) : this(
        script,
        KaPsiSymbolPointerCreator.symbolPointerOfType(script),
        ktModule,
    )

    private fun MutableList<PsiMethod>.addScriptDefaultMethods() {
        val defaultConstructor = SymbolLightMethodForScriptDefaultConstructor(
            script,
            this@SymbolLightClassForScript,
            METHOD_INDEX_FOR_DEFAULT_CTOR
        )
        add(defaultConstructor)

        val mainMethod = SymbolLightMethodForScriptMain(
            script,
            this@SymbolLightClassForScript,
            METHOD_INDEX_FOR_SCRIPT_MAIN
        )
        add(mainMethod)
    }

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        val result = mutableListOf<PsiMethod>()

        result.addScriptDefaultMethods()

        symbolPointer.withSymbol(ktModule) { scriptSymbol ->
            createMethods(this@SymbolLightClassForScript, scriptSymbol.declaredMemberScope.callables, result)
        }
        result
    }

    override fun getOwnFields(): List<PsiField> = cachedValue {
        symbolPointer.withSymbol(ktModule) { scriptSymbol ->
            buildList {
                addPropertyBackingFields(this@SymbolLightClassForScript, this, scriptSymbol, SymbolLightField.FieldNameGenerator())
            }
        }
    }

    override fun getOwnInnerClasses(): List<SymbolLightClassBase> = cachedValue {
        symbolPointer.withSymbol(ktModule) { scriptSymbol ->
            createInnerClasses(scriptSymbol, manager, this@SymbolLightClassForScript, classOrObject = null)
        }
    }

    override fun copy(): SymbolLightClassForScript =
        SymbolLightClassForScript(script, symbolPointer, ktModule)

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this@SymbolLightClassForScript,
            modifiersBox = InitializedModifiersBox(PsiModifier.PUBLIC, PsiModifier.FINAL)
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun hasModifierProperty(name: String): Boolean = _modifierList.hasModifierProperty(name)

    private val _containingFile by lazyPub {
        FakeFileForLightClass(
            script.containingKtFile,
            lightClass = this,
            packageFqName = script.fqName.parent(),
        )
    }

    override fun getContainingFile() = _containingFile

    override fun getName() = script.fqName.shortName().asString()

    override fun equals(other: Any?): Boolean = this === other || other is SymbolLightClassForScript && other.script == script

    override fun hashCode(): Int = script.hashCode()

    override fun toString(): String = "${SymbolLightClassForScript::class.java.simpleName}:${script.fqName}"

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun getContainingClass(): PsiClass? = null

    override fun isDeprecated(): Boolean = false

    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getQualifiedName() = script.fqName.asString()

    override fun isInterface(): Boolean = false

    override fun isAnnotationType(): Boolean = false

    override fun isEnum(): Boolean = false

    private val _extendsList: PsiReferenceList by lazyPub {
        KotlinSuperTypeListBuilder(
            this,
            kotlinOrigin = kotlinOrigin?.getSuperTypeList(),
            manager = manager,
            language = language,
            role = PsiReferenceList.Role.EXTENDS_LIST,
        ).apply {
            addReference("kotlin.script.templates.standard.ScriptTemplateWithArgs")
        }
    }

    override fun getExtendsList(): PsiReferenceList = _extendsList

    private val _implementsList by lazyPub {
        KotlinSuperTypeListBuilder(
            this,
            kotlinOrigin = kotlinOrigin?.getSuperTypeList(),
            manager = manager,
            language = language,
            role = PsiReferenceList.Role.IMPLEMENTS_LIST,
        )
    }

    override fun getImplementsList(): PsiReferenceList = _implementsList

    override fun getInterfaces(): Array<PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getSuperClass(): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, resolveScope)
    }

    override fun getSupers(): Array<PsiClass> {
        return superClass?.let { arrayOf(it) } ?: PsiClass.EMPTY_ARRAY
    }

    override fun getSuperTypes(): Array<PsiClassType> {
        return arrayOf(PsiType.getJavaLangObject(manager, resolveScope))
    }

    override fun getScope(): PsiElement = parent

    override fun isInheritorDeep(baseClass: PsiClass, classToByPass: PsiClass?): Boolean = false

}