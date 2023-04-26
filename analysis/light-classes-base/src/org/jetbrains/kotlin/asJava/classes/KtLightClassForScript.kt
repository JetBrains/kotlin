/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtScript
import javax.swing.Icon

abstract class KtLightClassForScript(val script: KtScript) : KtLightClassBase(script.manager) {
    private val modifierList: PsiModifierList = LightModifierList(
        manager,
        KotlinLanguage.INSTANCE,
        PsiModifier.PUBLIC, PsiModifier.FINAL
    )

    private val scriptImplementsList: LightEmptyImplementsList = LightEmptyImplementsList(manager)

    private val scriptExtendsList: PsiReferenceList by lazyPub {
        KotlinLightReferenceListBuilder(manager, PsiReferenceList.Role.EXTENDS_LIST).also {
            it.addReference("kotlin.script.templates.standard.ScriptTemplateWithArgs")
        }
    }

    private val _containingFile by lazyPub {
        FakeFileForLightClass(
            script.containingKtFile,
            lightClass = this,
            packageFqName = fqName.parent(),
        )
    }

    override val kotlinOrigin: KtClassOrObject? get() = null

    val fqName: FqName get() = script.fqName

    override fun getModifierList() = modifierList

    override fun hasModifierProperty(@NonNls name: String) = modifierList.hasModifierProperty(name)

    override fun isDeprecated() = false

    override fun isInterface() = false

    override fun isAnnotationType() = false

    override fun isEnum() = false

    override fun getContainingClass() = null

    override fun getContainingFile() = _containingFile

    override fun hasTypeParameters() = false

    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList() = null

    override fun getDocComment() = null

    override fun getImplementsList(): PsiReferenceList = scriptImplementsList

    override fun getExtendsList(): PsiReferenceList = scriptExtendsList

    override fun getImplementsListTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY

    override fun getInterfaces(): Array<PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getInitializers(): Array<PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY

    override fun getName() = script.fqName.shortName().asString()

    override fun getQualifiedName() = script.fqName.asString()

    override fun isValid() = script.isValid

    abstract override fun copy(): PsiElement

    override fun getNavigationElement() = script

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        equals(another) ||
                (another is KtLightClassForScript && fqName == another.fqName)

    override fun getElementIcon(flags: Int): Icon? =
        throw UnsupportedOperationException("This should be done by KotlinIconProvider")

    override val originKind: LightClassOriginKind get() = LightClassOriginKind.SOURCE

    override fun getLBrace(): PsiElement? = null

    override fun getRBrace(): PsiElement? = null

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun setName(name: String): PsiElement? = throw IncorrectOperationException()

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        return baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false

    override fun getSuperClass(): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, resolveScope)
    }

    override fun getSupers(): Array<PsiClass> {
        return superClass?.let { arrayOf(it) } ?: arrayOf()
    }

    override fun getSuperTypes(): Array<PsiClassType> {
        return arrayOf(PsiType.getJavaLangObject(manager, resolveScope))
    }

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun getParent(): PsiElement = containingFile

    override fun getScope(): PsiElement = parent

    override fun hashCode() = script.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null || this::class.java != other::class.java) {
            return false
        }

        val lightClass = other as? KtLightClassForScript ?: return false
        if (this === other) return true

        if (script != lightClass.script) return false

        return true
    }

    override fun toString() = "${KtLightClassForScript::class.java.simpleName}:${script.fqName}"
}
