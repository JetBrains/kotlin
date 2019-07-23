/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*

internal class KtUltraLightSuspendContinuationParameter(
    private val ktFunction: KtFunction,
    private val support: KtUltraLightSupport,
    method: KtLightMethod
) : LightParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, PsiType.NULL, method, method.language),
    KtLightParameter,
    KtUltraLightElementWithNullabilityAnnotation<KtParameter, PsiParameter> {

    override val kotlinTypeForNullabilityAnnotation: KotlinType? get() = ktType
    override val psiTypeForNullabilityAnnotation: PsiType? get() = psiType
    override val kotlinOrigin: KtParameter? = null
    override val clsDelegate: PsiParameter
        get() = throw IllegalStateException("Cls delegate shouldn't be loaded for ultra-light PSI!")

    private val ktType by lazyPub {
        val descriptor = ktFunction.resolve() as? FunctionDescriptor
        val returnType = descriptor?.returnType ?: return@lazyPub null
        support.moduleDescriptor.getContinuationOfTypeOrAny(returnType, support.isReleasedCoroutine)
    }

    private val psiType by lazyPub {
        ktType?.asPsiType(support, TypeMappingMode.DEFAULT, method) ?: PsiType.NULL
    }

    private val lightModifierList by lazyPub { KtLightSimpleModifierList(this, emptySet()) }

    override fun getType(): PsiType = psiType

    override fun equals(other: Any?): Boolean =
        other is KtUltraLightSuspendContinuationParameter && other.ktFunction === this.ktFunction

    override fun isVarArgs(): Boolean = false
    override fun hashCode(): Int = name.hashCode()
    override fun getModifierList(): PsiModifierList = lightModifierList
    override fun getNavigationElement(): PsiElement = ktFunction.navigationElement
    override fun getUseScope(): SearchScope = ktFunction.useScope
    override fun isValid() = ktFunction.isValid
    override fun getContainingFile(): PsiFile = ktFunction.containingFile
    override fun getParent(): PsiElement = method.parameterList

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        another is KtUltraLightSuspendContinuationParameter && another.psiType == this.psiType

    override fun copy(): PsiElement = KtUltraLightSuspendContinuationParameter(ktFunction, support, method)
}

internal abstract class KtUltraLightParameter(
    name: String,
    override val kotlinOrigin: KtParameter?,
    protected val support: KtUltraLightSupport,
    method: KtLightMethod
) : org.jetbrains.kotlin.asJava.elements.LightParameter(
    name,
    PsiType.NULL,
    method,
    method.language
), KtUltraLightElementWithNullabilityAnnotation<KtParameter, PsiParameter>, KtLightParameter {

    override fun isEquivalentTo(another: PsiElement?): Boolean = kotlinOrigin == another

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

        if (kotlinType.isSuspendFunctionType) {
            kotlinType.asPsiType(support, TypeMappingMode.DEFAULT, this)
        } else {
            val containingDescriptor = computeContainingDescriptor() ?: return@lazyPub PsiType.NULL
            support.mapType(this) { typeMapper, sw ->
                typeMapper.writeParameterType(sw, kotlinType, containingDescriptor)
            }
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
    kotlinOrigin: KtParameter?,
    support: KtUltraLightSupport,
    method: KtLightMethod,
    protected val containingDeclaration: KtCallableDeclaration
) : KtUltraLightParameter(name, kotlinOrigin, support, method) {
    override fun computeContainingDescriptor() = containingDeclaration.resolve() as? CallableMemberDescriptor
}

internal class KtUltraLightParameterForSource(
    name: String,
    override val kotlinOrigin: KtParameter,
    support: KtUltraLightSupport,
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
    support: KtUltraLightSupport,
    method: KtLightMethod,
    containingDeclaration: KtCallableDeclaration
) : KtAbstractUltraLightParameterForDeclaration(name, null, support, method, containingDeclaration) {

    override val kotlinType: KotlinType? by lazyPub { property.getKotlinType() }

    override fun isVarArgs(): Boolean = false
}

internal class KtUltraLightReceiverParameter(
    containingDeclaration: KtCallableDeclaration,
    support: KtUltraLightSupport,
    method: KtLightMethod
) : KtAbstractUltraLightParameterForDeclaration("\$self", null, support, method, containingDeclaration) {

    override fun isVarArgs(): Boolean = false

    override val kotlinType: KotlinType? by lazyPub { computeContainingDescriptor()?.extensionReceiverParameter?.type }
}

internal class KtUltraLightParameterForDescriptor(
    private val descriptor: ParameterDescriptor,
    support: KtUltraLightSupport,
    method: KtLightMethod
) : KtUltraLightParameter(
    if (descriptor.name.isSpecial) "\$self" else descriptor.name.identifier,
    null, support, method
) {
    override val kotlinType: KotlinType?
        get() = descriptor.type

    override fun computeContainingDescriptor() = descriptor.containingDeclaration as? CallableMemberDescriptor

    override fun isVarArgs() = (descriptor as? ValueParameterDescriptor)?.varargElementType != null

    override val givenAnnotations: List<KtLightAbstractAnnotation>
        get() = descriptor.obtainLightAnnotations(support, this)
}
