/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.psi.*

abstract class KtLightMethodImpl protected constructor(
    lightMemberOrigin: LightMemberOriginForDeclaration?,
    containingClass: KtLightClass,
) : KtLightMemberImpl<PsiMethod>(lightMemberOrigin, containingClass), KtLightMethod {
    private val calculatingReturnType = ThreadLocal<Boolean>()

    private val paramsList: PsiParameterList by lazyPub {
        val parameters = buildParametersForList()
        KtLightParameterList(this, parameters.size) {
            parameters
        }
    }

    protected abstract fun buildParametersForList(): List<PsiParameter>

    private val typeParamsList: PsiTypeParameterList? by lazyPub { buildTypeParameterList() }

    protected abstract fun buildTypeParameterList(): PsiTypeParameterList?

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitMethod(this)
        } else {
            visitor.visitElement(this)
        }
    }

    override val isMangled: Boolean get() = checkIsMangled()

    override fun setName(name: String): PsiElement? {
        val jvmNameAnnotation = modifierList.findAnnotation(JvmFileClassUtil.JVM_NAME.asString())?.unwrapped as? KtAnnotationEntry
        val demangledName = (if (isMangled) demangleInternalName(name) else null) ?: name
        val newNameForOrigin = propertyNameByAccessor(demangledName, this) ?: demangledName
        if (newNameForOrigin == kotlinOrigin?.name) {
            jvmNameAnnotation?.delete()
            return this
        }

        val nameExpression = jvmNameAnnotation?.let { JvmFileClassUtil.getLiteralStringEntryFromAnnotation(it) }
        if (nameExpression != null) {
            nameExpression.replace(KtPsiFactory(project).createLiteralStringTemplateEntry(name))
        } else {
            val toRename = kotlinOrigin as? PsiNamedElement ?: cannotModify()
            toRename.setName(newNameForOrigin)
        }

        return this
    }

    override fun delete() {
        kotlinOrigin?.let {
            if (it.isValid) {
                it.delete()
            }
        } ?: cannotModify()
    }

    abstract override fun getModifierList(): PsiModifierList

    override fun getParameterList() = paramsList

    override fun getTypeParameterList() = typeParamsList

    override fun getTypeParameters(): Array<PsiTypeParameter> =
        typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun hasTypeParameters() = typeParameters.isNotEmpty()

    abstract override fun getSignature(substitutor: PsiSubstitutor): MethodSignature

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean {
        return typeParameters.all { processor.execute(it, state) }
    }

    /* comparing origin and member index should be enough to determine equality:
        for compiled elements origin contains delegate
        for source elements index is unique to each member
    */
    override fun equals(other: Any?): Boolean = other === this ||
            other is KtLightMethodImpl &&
            other.javaClass == javaClass &&
            other.containingClass == containingClass &&
            other.lightMemberOrigin == lightMemberOrigin

    override fun hashCode(): Int = name.hashCode().times(31).plus(containingClass.hashCode())

    abstract override fun getDefaultValue(): PsiAnnotationMemberValue?

    abstract override fun getReturnTypeElement(): PsiTypeElement?

    override fun getReturnType(): PsiType? {
        calculatingReturnType.set(true)
        try {
            return returnTypeElement?.type
        } finally {
            calculatingReturnType.set(false)
        }
    }

    private inline fun <R> getTextVariantFromPropertyAccessorIfNeeded(
        retriever: (KtPropertyAccessor) -> R
    ): R? {
        val auxiliaryOrigin = lightMemberOrigin?.auxiliaryOriginalElement
        return (auxiliaryOrigin as? KtPropertyAccessor)?.let(retriever)
    }

    override fun getText(): String {
        return getTextVariantFromPropertyAccessorIfNeeded(KtPropertyAccessor::getText)
            ?: super.getText()
    }

    override fun getTextOffset(): Int {
        return getTextVariantFromPropertyAccessorIfNeeded(KtPropertyAccessor::getTextOffset)
            ?: super.getTextOffset()
    }

    override fun getTextRange(): TextRange {
        return getTextVariantFromPropertyAccessorIfNeeded(KtPropertyAccessor::getTextRange)
            ?: super.getTextRange()
    }

    abstract override fun getThrowsList(): PsiReferenceList

    abstract override fun isVarArgs(): Boolean

    abstract override fun isConstructor(): Boolean

    abstract override fun getHierarchicalMethodSignature(): HierarchicalMethodSignature

    abstract override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod>

    override fun getBody() = null

    @Suppress("OVERRIDE_DEPRECATION") // K2 warning suppression, TODO: KT-62472
    abstract override fun findDeepestSuperMethod(): PsiMethod?

    abstract override fun findDeepestSuperMethods(): Array<out PsiMethod>

    abstract override fun findSuperMethods(): Array<out PsiMethod>

    abstract override fun findSuperMethods(checkAccess: Boolean): Array<out PsiMethod>

    abstract override fun findSuperMethods(parentClass: PsiClass?): Array<out PsiMethod>
}

fun KtLightMethod.isTraitFakeOverride(): Boolean {
    val methodOrigin = this.kotlinOrigin
    if (!(methodOrigin is KtNamedFunction || methodOrigin is KtPropertyAccessor || methodOrigin is KtProperty)) {
        return false
    }

    val parentOfMethodOrigin = PsiTreeUtil.getParentOfType(methodOrigin, KtClassOrObject::class.java)
    val thisClassDeclaration = this.containingClass.kotlinOrigin

    // Method was generated from declaration in some other trait
    return (parentOfMethodOrigin != null && thisClassDeclaration !== parentOfMethodOrigin && KtPsiUtil.isTrait(parentOfMethodOrigin))
}

fun KtLightMethod.isAccessor(getter: Boolean): Boolean {
    val origin = kotlinOrigin as? KtCallableDeclaration ?: return false
    if (origin !is KtProperty && origin !is KtParameter) return false
    val expectedParametersCount = (if (getter) 0 else 1) + (if (origin.receiverTypeReference != null) 1 else 0)
    return parameterList.parametersCount == expectedParametersCount
}

val KtLightMethod.isGetter: Boolean
    get() = isAccessor(true)

val KtLightMethod.isSetter: Boolean
    get() = isAccessor(false)
