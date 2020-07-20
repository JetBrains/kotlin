/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.codegen.AsmUtil.LABELED_THIS_PARAMETER
import org.jetbrains.kotlin.codegen.AsmUtil.RECEIVER_PARAMETER_NAME
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget

internal class KtUltraLightSuspendContinuationParameter(
    private val ktFunction: KtFunction,
    private val support: KtUltraLightSupport,
    method: KtLightMethod
) : LightParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, PsiType.NULL, method, method.language),
    KtLightParameter,
    KtUltraLightElementWithNullabilityAnnotation<KtParameter, PsiParameter> {

    override val qualifiedNameForNullabilityAnnotation: String?
        get() = computeQualifiedNameForNullabilityAnnotation(ktType)

    override val psiTypeForNullabilityAnnotation: PsiType? get() = psiType
    override val kotlinOrigin: KtParameter? = null
    override val clsDelegate: PsiParameter get() = invalidAccess()

    private val ktType: KotlinType?
        get() {
            val descriptor = ktFunction.resolve() as? FunctionDescriptor
            val returnType = descriptor?.returnType ?: return null
            return support.moduleDescriptor.getContinuationOfTypeOrAny(returnType, support.isReleasedCoroutine)
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
    private val ultraLightMethod: KtUltraLightMethod
) : org.jetbrains.kotlin.asJava.elements.LightParameter(
    name,
    PsiType.NULL,
    ultraLightMethod,
    ultraLightMethod.language
), KtUltraLightElementWithNullabilityAnnotation<KtParameter, PsiParameter>, KtLightParameter {

    override fun isEquivalentTo(another: PsiElement?): Boolean = kotlinOrigin == another

    override val clsDelegate: PsiParameter get() = invalidAccess()

    private val lightModifierList by lazyPub { KtLightSimpleModifierList(this, emptySet()) }

    override fun getModifierList(): PsiModifierList = lightModifierList

    override fun getNavigationElement(): PsiElement = kotlinOrigin ?: method.navigationElement
    override fun getUseScope(): SearchScope = kotlinOrigin?.useScope ?: LocalSearchScope(this)

    override fun isValid() = parent.isValid

    override fun computeQualifiedNameForNullabilityAnnotation(kotlinType: KotlinType?): String? {
        val typeForAnnotation =
            if (isVarArgs && kotlinType != null && KotlinBuiltIns.isArray(kotlinType)) kotlinType.arguments[0].type else kotlinType
        return super.computeQualifiedNameForNullabilityAnnotation(typeForAnnotation)
    }

    override val psiTypeForNullabilityAnnotation: PsiType?
        get() = type


    protected fun computeParameterType(kotlinType: KotlinType?, containingDeclaration: CallableDescriptor?): PsiType {
        kotlinType ?: return PsiType.NULL

        if (kotlinType.isSuspendFunctionType) {
            return kotlinType.asPsiType(support, TypeMappingMode.DEFAULT, this)
        } else {
            val containingDescriptor = containingDeclaration ?: return PsiType.NULL
            val mappedType = support.mapType(this) { typeMapper, sw ->
                typeMapper.writeParameterType(sw, kotlinType, containingDescriptor)
            }
            return if (ultraLightMethod.checkNeedToErasureParametersTypes) TypeConversionUtil.erasure(mappedType) else mappedType
        }
    }

    abstract override fun getType(): PsiType

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
    method: KtUltraLightMethod,
    protected val containingDeclaration: KtCallableDeclaration
) : KtUltraLightParameter(name, kotlinOrigin, support, method) {

    protected fun tryGetContainingDescriptor(): CallableDescriptor? =
        containingDeclaration.resolve() as? CallableDescriptor

    protected abstract fun tryGetKotlinType(): KotlinType?

    private val _parameterType: PsiType by lazyPub {
        computeParameterType(tryGetKotlinType(), tryGetContainingDescriptor())
    }

    override fun getType(): PsiType = _parameterType

    override val qualifiedNameForNullabilityAnnotation: String? by lazyPub {
        computeQualifiedNameForNullabilityAnnotation(tryGetKotlinType())
    }
}

internal class KtUltraLightParameterForSource(
    name: String,
    override val kotlinOrigin: KtParameter,
    support: KtUltraLightSupport,
    method: KtUltraLightMethod,
    containingDeclaration: KtCallableDeclaration
) : KtAbstractUltraLightParameterForDeclaration(name, kotlinOrigin, support, method, containingDeclaration) {

    override fun tryGetKotlinType(): KotlinType? = kotlinOrigin.getKotlinType()

    override fun isVarArgs(): Boolean = kotlinOrigin.isVarArg && method.parameterList.parameters.last() == this

    override fun setName(@NonNls name: String): PsiElement {
        kotlinOrigin.setName(name)
        return this
    }

    override fun getText(): String? = kotlinOrigin.text
    override fun getTextRange(): TextRange = kotlinOrigin.textRange
    override fun getTextOffset(): Int = kotlinOrigin.textOffset
    override fun getStartOffsetInParent(): Int = kotlinOrigin.startOffsetInParent
    override fun isWritable(): Boolean = kotlinOrigin.isWritable
    override fun getNavigationElement(): PsiElement = kotlinOrigin.navigationElement
    override fun getContainingFile(): PsiFile = parent.containingFile
    override fun getPresentation(): ItemPresentation? = kotlinOrigin.let { ItemPresentationProviders.getItemPresentation(it) }
    override fun findElementAt(offset: Int): PsiElement? = kotlinOrigin.findElementAt(offset)
}

internal class KtUltraLightParameterForSetterParameter(
    name: String,
    // KtProperty or KtParameter from primary constructor
    private val property: KtDeclaration,
    support: KtUltraLightSupport,
    method: KtUltraLightMethod,
    containingDeclaration: KtCallableDeclaration
) : KtAbstractUltraLightParameterForDeclaration(name, null, support, method, containingDeclaration) {

    override fun tryGetKotlinType(): KotlinType? = property.getKotlinType()

    override val givenAnnotations: List<KtLightAbstractAnnotation>?
        get() = property.annotationEntries.toLightAnnotations(this, AnnotationUseSiteTarget.SETTER_PARAMETER)

    override fun isVarArgs(): Boolean = false
}

internal class KtUltraLightReceiverParameter(
    containingDeclaration: KtCallableDeclaration,
    support: KtUltraLightSupport,
    method: KtUltraLightMethod
) : KtAbstractUltraLightParameterForDeclaration(
    /** @see org.jetbrains.kotlin.codegen.AsmUtil.getNameForReceiverParameter */
    name = AsmUtil.getLabeledThisName(method.name, LABELED_THIS_PARAMETER, RECEIVER_PARAMETER_NAME),
    kotlinOrigin = null,
    support = support,
    method = method,
    containingDeclaration = containingDeclaration
) {

    override val givenAnnotations: List<KtLightAbstractAnnotation>? =
        containingDeclaration
            .receiverTypeReference
            ?.modifierList
            ?.annotationEntries
            ?.toLightAnnotations(this, AnnotationUseSiteTarget.RECEIVER)
            ?: emptyList()

    override fun isVarArgs(): Boolean = false

    override fun tryGetKotlinType(): KotlinType? =
        tryGetContainingDescriptor()?.extensionReceiverParameter?.type
}

internal class KtUltraLightParameterForDescriptor(
    descriptor: ParameterDescriptor,
    support: KtUltraLightSupport,
    method: KtUltraLightMethod
) : KtUltraLightParameter(
    if (descriptor.name.isSpecial) "\$self" else descriptor.name.identifier,
    null, support, method
) {
    // This is greedy realization of UL class.
    // This means that all data that depends on descriptor evaluated in ctor so the descriptor will be released on the end.
    // Be aware to save descriptor in class instance or any depending references

    private val lazyInitializers = mutableListOf<Lazy<*>>()
    private inline fun <T> getAndAddLazy(crossinline initializer: () -> T): Lazy<T> =
        lazyPub { initializer() }.also { lazyInitializers.add(it) }

    override val qualifiedNameForNullabilityAnnotation: String? by getAndAddLazy {
        computeQualifiedNameForNullabilityAnnotation(descriptor.type)
    }

    private val _isVarArgs: Boolean by getAndAddLazy {
        (descriptor as? ValueParameterDescriptor)?.varargElementType != null
    }

    override fun isVarArgs() = _isVarArgs

    override val givenAnnotations: List<KtLightAbstractAnnotation> by getAndAddLazy {
        descriptor.obtainLightAnnotations(support, this)
    }

    private val _parameterType by getAndAddLazy {
        computeParameterType(descriptor.type, descriptor.containingDeclaration as? CallableMemberDescriptor)
    }

    override fun getType(): PsiType = _parameterType

    init {
        //We should force computations on all lazy delegates to release descriptor on the end of ctor call
        with(lazyInitializers) {
            forEach { it.value }
            clear()
        }
    }
}
