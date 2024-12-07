/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightReferenceListBuilder
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.checkIsMangled
import org.jetbrains.kotlin.asJava.classes.KotlinLightReferenceListBuilder
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.mangleInternalName
import org.jetbrains.kotlin.light.classes.symbol.SymbolLightMemberBase
import org.jetbrains.kotlin.light.classes.symbol.annotations.getJvmNameFromAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasPublishedApiAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase

internal abstract class SymbolLightMethodBase(
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    protected val methodIndex: Int,
) : SymbolLightMemberBase<PsiMethod>(lightMemberOrigin, containingClass), KtLightMethod {
    override fun getBody(): PsiCodeBlock? = null

    override fun getReturnTypeElement(): PsiTypeElement? = null

    override fun setName(name: String): PsiElement = cannotModify()

    override fun isVarArgs() = PsiImplUtil.isVarArgs(this)

    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)

    @Deprecated("Deprecated in Java")
    override fun findDeepestSuperMethod(): PsiMethod? = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)

    override fun findDeepestSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findDeepestSuperMethods(this)

    override fun findSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this)

    override fun findSuperMethods(checkAccess: Boolean): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)

    override fun findSuperMethods(parentClass: PsiClass?): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature =
        MethodSignatureBackedByPsiMethod.create(this, substitutor)

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement,
    ): Boolean {
        return PsiImplUtil.processDeclarationsInMethod(this, processor, state, lastParent, place)
    }

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitMethod(this)
        } else {
            visitor.visitElement(this)
        }
    }

    override val isMangled: Boolean get() = checkIsMangled()

    abstract override fun getTypeParameters(): Array<PsiTypeParameter>
    abstract override fun hasTypeParameters(): Boolean
    abstract override fun getTypeParameterList(): PsiTypeParameterList?

    private class SymbolLightThrowsReferencesListBuilder(
        private val parentMethod: PsiMethod
    ) : KotlinLightReferenceListBuilder(parentMethod.manager, parentMethod.language, PsiReferenceList.Role.THROWS_LIST) {
        override fun getParent(): PsiElement = parentMethod

        override fun getContainingFile(): PsiFile = parentMethod.containingFile
    }

    private val _throwsList by lazyPub {
        val builder = SymbolLightThrowsReferencesListBuilder(this)
        computeThrowsList(builder)
        builder
    }

    protected open fun computeThrowsList(builder: LightReferenceListBuilder) {}

    override fun getThrowsList(): PsiReferenceList = _throwsList

    override fun getDefaultValue(): PsiAnnotationMemberValue? = null

    protected fun KaSession.computeJvmMethodName(
        symbol: KaCallableSymbol,
        defaultName: String,
    ): String {
        symbol.getJvmNameFromAnnotation()?.let { return it }

        if (containingClass is KtLightClassForFacade) return defaultName
        val sourceModule = ktModule as? KaSourceModule ?: return defaultName

        if (symbol.hasPublishedApiAnnotation()) return defaultName
        if (symbol.visibility != KaSymbolVisibility.INTERNAL) return defaultName
        return mangleInternalName(defaultName, sourceModule.stableModuleName ?: sourceModule.name)
    }

    abstract fun isOverride(): Boolean

    internal open fun suppressWildcards(): Boolean? = null
}
