/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightTypeParameterListBuilder
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.asJava.elements.KtLightSimpleModifierList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType

internal abstract class KtUltraLightMethod(
    internal val delegate: LightMethodBuilder,
    closestDeclarationForOrigin: KtDeclaration?,
    protected val support: UltraLightSupport,
    containingClass: KtUltraLightClass
) : KtLightMethodImpl(
    { delegate },
    closestDeclarationForOrigin?.let {
        LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.OTHER)
    },
    containingClass
), KtUltraLightElementWithNullabilityAnnotation<KtDeclaration, PsiMethod> {

    override val psiTypeForNullabilityAnnotation: PsiType?
        get() = returnType

    // These two overrides are necessary because ones from KtLightMethodImpl suppose that clsDelegate.returnTypeElement is valid
    // While here we only set return type for LightMethodBuilder (see org.jetbrains.kotlin.asJava.classes.KtUltraLightClass.asJavaMethod)
    override fun getReturnTypeElement(): PsiTypeElement? = null

    override fun getReturnType(): PsiType? = clsDelegate.returnType

    override fun buildParametersForList(): List<PsiParameter> = clsDelegate.parameterList.parameters.toList()

    // should be in super
    override fun isVarArgs() = PsiImplUtil.isVarArgs(this)

    abstract override fun buildTypeParameterList(): PsiTypeParameterList

    private val _throwsList: PsiReferenceList by lazyPub {
        val list =
            object : KotlinLightReferenceListBuilder(manager, language, PsiReferenceList.Role.THROWS_LIST) {
                override fun getParent() = this@KtUltraLightMethod
                override fun getContainingFile() = this@KtUltraLightMethod.containingFile
            }
        computeDescriptor()?.let {
            for (ex in FunctionCodegen.getThrownExceptions(it)) {
                val psiClassType = ex.defaultType.asPsiType(support, TypeMappingMode.DEFAULT, list) as? PsiClassType ?: continue
                list.addReference(psiClassType)
            }
        }
        list
    }

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

    override fun getThrowsList(): PsiReferenceList = _throwsList

    abstract fun computeDescriptor(): FunctionDescriptor?
}

internal class KtUltraLightMethodForSourceDeclaration(
    delegate: LightMethodBuilder,
    declaration: KtDeclaration,
    support: UltraLightSupport,
    containingClass: KtUltraLightClass
) : KtUltraLightMethod(
    delegate,
    declaration,
    support, containingClass
) {
    override val kotlinTypeForNullabilityAnnotation: KotlinType?
        get() = kotlinOrigin?.getKotlinType()

    override fun buildTypeParameterList(): PsiTypeParameterList {
        val origin = kotlinOrigin
        return if (origin is KtFunction || origin is KtProperty)
            buildTypeParameterList(origin as KtTypeParameterListOwner, this, support)
        else LightTypeParameterListBuilder(manager, language)
    }

    override fun computeDescriptor() = kotlinOrigin?.resolve() as? FunctionDescriptor
}

internal class KtUltraLightMethodForDescriptor(
    private val descriptor: FunctionDescriptor,
    delegate: LightMethodBuilder,
    closestDeclarationForOrigin: KtDeclaration?,
    support: UltraLightSupport,
    containingClass: KtUltraLightClass
) : KtUltraLightMethod(
    delegate,
    closestDeclarationForOrigin,
    support,
    containingClass
) {
    override fun buildTypeParameterList() = buildTypeParameterList(descriptor, this, support)
    override fun computeDescriptor() = descriptor

    override val kotlinTypeForNullabilityAnnotation: KotlinType?
        get() = descriptor.returnType

    override val givenAnnotations: List<KtLightAbstractAnnotation>
        get() = descriptor.obtainLightAnnotations(support, this)
}

internal abstract class KtUltraLightParameter(
    name: String,
    override val kotlinOrigin: KtDeclaration?,
    protected val support: UltraLightSupport,
    method: KtLightMethod
) : org.jetbrains.kotlin.asJava.elements.LightParameter(
    name,
    PsiType.NULL,
    method,
    method.language
), KtUltraLightElementWithNullabilityAnnotation<KtDeclaration, PsiParameter> {

    override val clsDelegate: PsiParameter
        get() = throw IllegalStateException("Cls delegate shouldn't be loaded for ultra-light PSI!")

    private val lightModifierList by lazyPub { KtLightSimpleModifierList(this, emptySet()) }

    override fun getModifierList(): PsiModifierList = lightModifierList

    override fun getNavigationElement(): PsiElement = kotlinOrigin ?: method.navigationElement
    override fun getUseScope(): SearchScope = kotlinOrigin?.useScope ?: LocalSearchScope(this)

    override fun isValid() = parent.isValid

    protected abstract val kotlinType: KotlinType?
    protected abstract fun computeContainingDescriptor(): CallableDescriptor?

    override val kotlinTypeForNullabilityAnnotation: KotlinType?
        get() {
            val type = kotlinType
            return if (isVarArgs && type != null && KotlinBuiltIns.isArray(type)) {
                type.arguments[0].type
            } else {
                type
            }
        }

    override val psiTypeForNullabilityAnnotation: PsiType?
        get() = type


    private val _type: PsiType by lazyPub {
        val kotlinType = kotlinType ?: return@lazyPub PsiType.NULL
        val containingDescriptor = computeContainingDescriptor() ?: return@lazyPub PsiType.NULL
        support.mapType(this) { typeMapper, sw ->
            typeMapper.writeParameterType(sw, kotlinType, containingDescriptor)
        }
    }

    override fun getType(): PsiType = _type

    override fun getContainingFile(): PsiFile = method.containingFile
    override fun getParent(): PsiElement = method.parameterList

    override fun equals(other: Any?): Boolean =
        other is KtUltraLightParameter && other.kotlinOrigin == this.kotlinOrigin

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    abstract override fun isVarArgs(): Boolean
}

internal abstract class KtAbstractUltraLightParameterForDeclaration(
    name: String,
    kotlinOrigin: KtDeclaration?,
    support: UltraLightSupport,
    method: KtLightMethod,
    protected val containingDeclaration: KtCallableDeclaration
) : KtUltraLightParameter(name, kotlinOrigin, support, method) {
    override fun computeContainingDescriptor() = containingDeclaration.resolve() as? CallableMemberDescriptor
}

internal class KtUltraLightParameterForSource(
    name: String,
    override val kotlinOrigin: KtParameter,
    support: UltraLightSupport,
    method: KtLightMethod,
    containingDeclaration: KtCallableDeclaration
) : KtAbstractUltraLightParameterForDeclaration(name, kotlinOrigin, support, method, containingDeclaration) {

    override val kotlinType: KotlinType? by lazyPub {
        kotlinOrigin.getKotlinType()
    }

    override fun isVarArgs(): Boolean = kotlinOrigin.isVarArg && method.parameterList.parameters.last() == this

    override fun setName(@NonNls name: String): PsiElement {
        kotlinOrigin.setName(name)
        return this
    }
}

internal class KtUltraLightParameterForSetterParameter(
    name: String,
    // KtProperty or KtParameter from primary constructor
    private val property: KtDeclaration,
    support: UltraLightSupport,
    method: KtLightMethod,
    containingDeclaration: KtCallableDeclaration
) : KtAbstractUltraLightParameterForDeclaration(name, null, support, method, containingDeclaration) {

    override val kotlinType: KotlinType? by lazyPub {
        property.getKotlinType()
    }

    override fun isVarArgs(): Boolean = false
}

internal class KtUltraLightReceiverParameter(
    containingDeclaration: KtCallableDeclaration,
    support: UltraLightSupport,
    method: KtLightMethod
) : KtAbstractUltraLightParameterForDeclaration("\$self", null, support, method, containingDeclaration) {

    override fun isVarArgs(): Boolean = false

    override val kotlinType: KotlinType? by lazyPub {
        computeContainingDescriptor()?.extensionReceiverParameter?.type
    }
}

internal class KtUltraLightParameterForDescriptor(
    private val descriptor: ParameterDescriptor,
    kotlinOrigin: KtDeclaration?,
    support: UltraLightSupport,
    method: KtLightMethod
) : KtUltraLightParameter(
    if (descriptor.name.isSpecial) "\$self" else descriptor.name.identifier,
    kotlinOrigin, support, method
) {
    override val kotlinType: KotlinType?
        get() = descriptor.type

    override fun computeContainingDescriptor() = descriptor.containingDeclaration as? CallableMemberDescriptor

    override fun isVarArgs() = (descriptor as? ValueParameterDescriptor)?.varargElementType != null

    override val givenAnnotations: List<KtLightAbstractAnnotation>
        get() = descriptor.obtainLightAnnotations(support, this)
}
