/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KotlinLightReferenceListBuilder
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.idea.util.module

internal abstract class FirLightMethod(
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: KtLightClass,
    private val methodIndex: Int
) : FirLightMemberImpl<PsiMethod>(lightMemberOrigin, containingClass), KtLightMethod {

    override fun getBody(): PsiCodeBlock? = null

    override fun getReturnTypeElement(): PsiTypeElement? = null

    override fun setName(p0: String): PsiElement = cannotModify()

    override fun isVarArgs() = PsiImplUtil.isVarArgs(this)

    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)

    override fun findDeepestSuperMethod() = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)

    override fun findDeepestSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findDeepestSuperMethods(this)

    override fun findSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this)

    override fun findSuperMethods(checkAccess: Boolean): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)

    override fun findSuperMethods(parentClass: PsiClass?): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature =
        MethodSignatureBackedByPsiMethod.create(this, substitutor)

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitMethod(this)
        } else {
            visitor.visitElement(this)
        }
    }

    override val isMangled: Boolean = false

    abstract override fun getTypeParameters(): Array<PsiTypeParameter>
    abstract override fun hasTypeParameters(): Boolean
    abstract override fun getTypeParameterList(): PsiTypeParameterList?

    override fun getThrowsList(): PsiReferenceList =
        KotlinLightReferenceListBuilder(manager, language, PsiReferenceList.Role.THROWS_LIST) //TODO()

    override fun getDefaultValue(): PsiAnnotationMemberValue? = null //TODO()

    protected fun <T> T.computeJvmMethodName(
        defaultName: String,
        containingClass: FirLightClassBase,
        annotationUseSiteTarget: AnnotationUseSiteTarget? = null
    ): String where T : KtAnnotatedSymbol, T : KtSymbolWithVisibility, T : KtCallableSymbol {
        getJvmNameFromAnnotation(annotationUseSiteTarget)?.let { return it }

        val effectiveVisibilityIfNotInternal = (visibility != Visibilities.Internal).ifTrue {
            (containingClass as? FirLightClassForSymbol)?.tryGetEffectiveVisibility(this)
        } ?: this.visibility

        if (effectiveVisibilityIfNotInternal != Visibilities.Internal) return defaultName

        val moduleName = module?.name ?: return defaultName

        if (hasPublishedApiAnnotation(annotationUseSiteTarget)) return defaultName

        return KotlinTypeMapper.InternalNameMapper.mangleInternalName(defaultName, moduleName)
    }
}