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

import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.builder.MemberIndex
import org.jetbrains.kotlin.asJava.builder.memberIndex
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.propertyNameByAccessor
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

open class KtLightMethodImpl protected constructor(
        computeRealDelegate: () -> PsiMethod,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: KtLightClass,
        private val dummyDelegate: PsiMethod? = null
) : KtLightMemberImpl<PsiMethod>(computeRealDelegate, lightMemberOrigin, containingClass, dummyDelegate), KtLightMethod {
    private val returnTypeElem by lazyPub {
        val delegateTypeElement = clsDelegate.returnTypeElement as? ClsTypeElementImpl
        delegateTypeElement?.let { ClsTypeElementImpl(this, it.canonicalText, /*ClsTypeElementImpl.VARIANCE_NONE */ 0.toChar()) }
    }

    private val calculatingReturnType = ThreadLocal<Boolean>()

    private val paramsList: PsiParameterList by lazyPub {
        KtLightParameterList(this, dummyDelegate?.parameterList?.parametersCount ?: clsDelegate.parameterList.parametersCount) {
            clsDelegate.parameterList.parameters.mapIndexed { index, clsParameter -> KtLightParameter(clsParameter, index, this@KtLightMethodImpl) }
        }
    }

    private val typeParamsList: PsiTypeParameterList? by lazyPub { buildTypeParameterList() }

    protected open fun buildTypeParameterList(): PsiTypeParameterList? {
        val origin = (lightMemberOrigin as? LightMemberOriginForDeclaration)?.originalElement
        return when {
            origin is KtClassOrObject -> KotlinLightTypeParameterListBuilder(this)
            origin != null -> LightClassUtil.buildLightTypeParameterList(this, origin)
            else -> clsDelegate.typeParameterList
        }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitMethod(this)
        }
        else {
            visitor.visitElement(this)
        }
    }

    override val isMangled: Boolean
        get() {
            val demangledName = KotlinTypeMapper.InternalNameMapper.demangleInternalName(name) ?: return false
            val originalName = propertyNameByAccessor(demangledName, this) ?: demangledName
            return originalName == kotlinOrigin?.name
        }

    override fun setName(name: String): PsiElement? {
        val jvmNameAnnotation = modifierList.findAnnotation(DescriptorUtils.JVM_NAME.asString())
        val demangledName = (if (isMangled) KotlinTypeMapper.InternalNameMapper.demangleInternalName(name) else null) ?: name
        val newNameForOrigin = propertyNameByAccessor(demangledName, this) ?: demangledName
        if (newNameForOrigin == kotlinOrigin?.name) {
            jvmNameAnnotation?.delete()
            return this
        }
        val nameExpression = jvmNameAnnotation?.findAttributeValue("name")?.unwrapped as? KtStringTemplateExpression
        if (nameExpression != null) {
            nameExpression.replace(KtPsiFactory(this).createStringTemplate(name))
        }
        else {
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

    override fun getModifierList(): PsiModifierList {
        if (calculatingReturnType.get() == true) {
            return KotlinJavaPsiFacade.getInstance(project).emptyModifierList
        }
        return super.getModifierList()
    }

    override fun getParameterList() = paramsList

    override fun getTypeParameterList() = typeParamsList

    override fun getTypeParameters(): Array<PsiTypeParameter> =
            typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature {
        if (substitutor == PsiSubstitutor.EMPTY) {
            return clsDelegate.getSignature(substitutor)
        }
        return MethodSignatureBackedByPsiMethod.create(this, substitutor)
    }

    override fun copy(): PsiElement {
        return Factory.create(clsDelegate, lightMemberOrigin?.copy(), containingClass)
    }

    override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
        return typeParameters.all { processor.execute(it, state) }
    }

    private val _memberIndex: MemberIndex?
        get() = (dummyDelegate ?: clsDelegate).memberIndex

    /* comparing origin and member index should be enough to determine equality:
            for compiled elements origin contains delegate
            for source elements index is unique to each member
            */
    override fun equals(other: Any?): Boolean =
            this === other ||
            (other is KtLightMethodImpl &&
             this.name == other.name &&
             this.containingClass == other.containingClass &&
             this.lightMemberOrigin == other.lightMemberOrigin &&
             this._memberIndex == other._memberIndex)

    override fun hashCode(): Int = ((getName().hashCode() * 31 + (lightMemberOrigin?.hashCode() ?: 0)) * 31 + containingClass.hashCode()) * 31 + (_memberIndex?.hashCode() ?: 0)

    override fun getDefaultValue() = (clsDelegate as? PsiAnnotationMethod)?.defaultValue

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
        private fun adjustMethodOrigin(origin: LightMemberOriginForDeclaration?): LightMemberOriginForDeclaration? {
            val originalElement = origin?.originalElement
            if (originalElement is KtPropertyAccessor) {
                return origin.copy(originalElement.getStrictParentOfType<KtProperty>()!!, origin.originKind)
            }
            return origin
        }

        fun create(
                delegate: PsiMethod, origin: LightMemberOrigin?, containingClass: KtLightClass
        ): KtLightMethodImpl {
            return KtLightMethodImpl({ delegate}, origin, containingClass)
        }

        fun lazy(
                dummyDelegate: PsiMethod?,
                containingClass: KtLightClass,
                origin: LightMemberOriginForDeclaration?,
                computeRealDelegate: () -> PsiMethod
        ): KtLightMethodImpl {
            return KtLightMethodImpl(computeRealDelegate, origin, containingClass, dummyDelegate)
        }

        fun fromClsMethods(delegateClass: PsiClass, containingClass: KtLightClass) = delegateClass.methods.map {
            KtLightMethodImpl.create(it, getOrigin(it), containingClass)
        }

        fun getOrigin(method: PsiMethod) = adjustMethodOrigin(getMemberOrigin(method))
    }

    override fun getThrowsList() = clsDelegate.throwsList

    override fun hasTypeParameters() = clsDelegate.hasTypeParameters()

    override fun isVarArgs() = (dummyDelegate ?: clsDelegate).isVarArgs

    override fun isConstructor() = dummyDelegate?.isConstructor ?: clsDelegate.isConstructor

    override fun getHierarchicalMethodSignature() = clsDelegate.hierarchicalMethodSignature

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean) = clsDelegate.findSuperMethodSignaturesIncludingStatic(checkAccess)

    override fun getBody() = null

    override fun findDeepestSuperMethod() = clsDelegate.findDeepestSuperMethod()

    override fun findDeepestSuperMethods() = clsDelegate.findDeepestSuperMethods()

    override fun findSuperMethods() = clsDelegate.findSuperMethods()

    override fun findSuperMethods(checkAccess: Boolean) = clsDelegate.findSuperMethods(checkAccess)

    override fun findSuperMethods(parentClass: PsiClass?) = clsDelegate.findSuperMethods(parentClass)
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