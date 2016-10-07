/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.core.JavaCoreBundle
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.propertyNameByAccessor
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

interface KtLightMethod : PsiMethod, KtLightDeclaration<KtDeclaration, PsiMethod> {
    val lightMethodOrigin: LightMemberOrigin?
    val isDelegated: Boolean
}

sealed class KtLightMethodImpl(
        override val clsDelegate: PsiMethod,
        override val lightMethodOrigin: LightMemberOrigin?,
        containingClass: KtLightClass
) : LightMethod(clsDelegate.manager, clsDelegate, containingClass), KtLightMethod {
    override val kotlinOrigin: KtDeclaration? get() = lightMethodOrigin?.originalElement as? KtDeclaration

    private val lightIdentifier by lazy(LazyThreadSafetyMode.PUBLICATION) { KtLightIdentifier(this, kotlinOrigin as? KtNamedDeclaration) }
    private val returnTypeElem by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val delegateTypeElement = clsDelegate.returnTypeElement as? ClsTypeElementImpl
        delegateTypeElement?.let { ClsTypeElementImpl(this, it.canonicalText, /*ClsTypeElementImpl.VARIANCE_NONE */ 0.toChar()) }
    }

    private val calculatingReturnType = ThreadLocal<Boolean>()

    override fun getContainingClass(): KtLightClass = super.getContainingClass() as KtLightClass

    private val paramsList: CachedValue<PsiParameterList> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val cacheManager = CachedValuesManager.getManager(clsDelegate.project)
        cacheManager.createCachedValue<PsiParameterList>({
            val parameterBuilder = LightParameterListBuilder(manager, KotlinLanguage.INSTANCE, this)

            for ((index, parameter) in clsDelegate.parameterList.parameters.withIndex()) {
                parameterBuilder.addParameter(KtLightParameter(parameter, index, this))
            }

            CachedValueProvider.Result.create(parameterBuilder, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }, false)
    }

    private val typeParamsList: CachedValue<PsiTypeParameterList> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val cacheManager = CachedValuesManager.getManager(clsDelegate.project)
        cacheManager.createCachedValue<PsiTypeParameterList>({
             val origin = (lightMethodOrigin as? LightMemberOriginForDeclaration)?.originalElement
             val list = if (origin != null) {
                 if (origin is KtClassOrObject) {
                     KotlinLightTypeParameterListBuilder(manager)
                 }
                 else {
                     LightClassUtil.buildLightTypeParameterList(this@KtLightMethodImpl, origin)
                 }
             }
             else {
                 clsDelegate.typeParameterList
             }
             CachedValueProvider.Result.create(list, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
         }, false)
    }

    override fun getNavigationElement(): PsiElement = kotlinOrigin?.navigationElement ?: super.getNavigationElement()
    override fun getParent(): PsiElement? = containingClass
    override fun getText() = kotlinOrigin?.text ?: ""
    override fun getTextRange() = kotlinOrigin?.textRange ?: TextRange.EMPTY_RANGE

    override val isDelegated: Boolean
        get() = lightMethodOrigin?.originKind == JvmDeclarationOriginKind.DELEGATION
                || lightMethodOrigin?.originKind == JvmDeclarationOriginKind.DELEGATION_TO_DEFAULT_IMPLS

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitMethod(this)
        }
        else {
            visitor.visitElement(this)
        }
    }

    override fun setName(name: String): PsiElement? {
        val jvmNameAnnotation = modifierList.findAnnotation(DescriptorUtils.JVM_NAME.asString())
        val newNameForOrigin = propertyNameByAccessor(name, this) ?: name
        if (newNameForOrigin == kotlinOrigin?.name) {
            jvmNameAnnotation?.delete()
            return this
        }
        val nameExpression = jvmNameAnnotation?.findAttributeValue("name")?.unwrapped as? KtStringTemplateExpression
        if (nameExpression != null) {
            nameExpression.replace(KtPsiFactory(this).createStringTemplate(name))
        }
        else {
            val toRename = kotlinOrigin as? PsiNamedElement ?: throwCanNotModify()
            toRename.setName(newNameForOrigin)
        }
        return this
    }

    override fun delete() {
        kotlinOrigin?.let {
            if (it.isValid) {
                it.delete()
            }
        } ?: throwCanNotModify()
    }

    private fun throwCanNotModify(): Nothing {
        throw IncorrectOperationException(JavaCoreBundle.message("psi.error.attempt.to.edit.class.file"))
    }

    private val _modifierList by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (lightMethodOrigin is LightMemberOriginForDeclaration)
            KtLightModifierList(clsDelegate.modifierList, this)
        else clsDelegate.modifierList
    }

    override fun getModifierList(): PsiModifierList {
        if (calculatingReturnType.get() == true) {
            return KotlinJavaPsiFacade.getInstance(project).emptyModifierList
        }
        return _modifierList
    }

    override fun getNameIdentifier() = lightIdentifier

    override fun getParameterList() = paramsList.value

    override fun getTypeParameterList() = typeParamsList.value

    override fun getTypeParameters(): Array<PsiTypeParameter> =
            typeParameterList?.let { it.typeParameters } ?: PsiTypeParameter.EMPTY_ARRAY

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature {
        if (substitutor == PsiSubstitutor.EMPTY) {
            return clsDelegate.getSignature(substitutor)
        }
        return MethodSignatureBackedByPsiMethod.create(this, substitutor)
    }

    override fun copy(): PsiElement {
        return Factory.create(clsDelegate, lightMethodOrigin?.copy(), containingClass)
    }

    override fun getUseScope() = kotlinOrigin?.useScope ?: super.getUseScope()

    override fun getLanguage() = KotlinLanguage.INSTANCE

    override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
        return typeParameters.all { processor.execute(it, state) }
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is KtLightMethod && kotlinOrigin == another.kotlinOrigin && clsDelegate == another.clsDelegate) {
            return true
        }

        return super.isEquivalentTo(another)
    }

    override fun equals(other: Any?): Boolean =
            other is KtLightMethod &&
            name == other.name &&
            lightMethodOrigin == other.lightMethodOrigin &&
            containingClass == other.containingClass &&
            clsDelegate == other.clsDelegate

    override fun hashCode(): Int = ((name.hashCode() * 31 + (lightMethodOrigin?.hashCode() ?: 0)) * 31 + containingClass.hashCode()) * 31 + clsDelegate.hashCode()

    override fun toString(): String = "${this.javaClass.simpleName}:$name"

    private class KtLightMethodForDeclaration(
            delegate: PsiMethod, origin: LightMemberOrigin?, containingClass: KtLightClass
    ) : KtLightMethodImpl(delegate, origin, containingClass)

    private class KtLightAnnotationMethod(
            override val clsDelegate: PsiAnnotationMethod,
            origin: LightMemberOrigin?,
            containingClass: KtLightClass
    ) : KtLightMethodImpl(clsDelegate, origin, containingClass), PsiAnnotationMethod {
        override fun getDefaultValue() = clsDelegate.defaultValue
    }

    // override getReturnType() so return type resolves to type parameters of this method not delegate's
    // which is relied upon by java type inference
    override fun getReturnTypeElement(): PsiTypeElement? = returnTypeElem

    override fun getReturnType(): PsiType? {
        calculatingReturnType.set(true)
        try {
            return returnTypeElement?.type
        }
        finally {
            calculatingReturnType.set(false)
        }
    }

    companion object Factory {
        fun create(
                delegate: PsiMethod, origin: LightMemberOrigin?, containingClass: KtLightClass
        ): KtLightMethodImpl {
            return when (delegate) {
                is PsiAnnotationMethod -> KtLightAnnotationMethod(delegate, origin, containingClass)
                else -> KtLightMethodForDeclaration(delegate, origin, containingClass)
            }
        }
    }
}

fun KtLightMethod.isTraitFakeOverride(): Boolean {
    val methodOrigin = this.kotlinOrigin
    if (!(methodOrigin is KtNamedFunction || methodOrigin is KtPropertyAccessor || methodOrigin is KtProperty)) {
        return false
    }

    val parentOfMethodOrigin = PsiTreeUtil.getParentOfType(methodOrigin, KtClassOrObject::class.java)
    val thisClassDeclaration = (this.containingClass as KtLightClass).kotlinOrigin

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