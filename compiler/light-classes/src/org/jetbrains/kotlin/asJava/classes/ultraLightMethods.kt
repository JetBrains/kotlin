/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightTypeParameterListBuilder
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.asJava.elements.KtUltraLightModifierList
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.getSpecialSignatureInfo
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasBody
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.RawType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private class KtUltraLightMethodModifierList(
    support: KtUltraLightSupport,
    owner: KtUltraLightMethod,
    val delegate: PsiMethod,
) : KtUltraLightModifierList<KtUltraLightMethod>(owner, support) {
    override fun hasModifierProperty(name: String): Boolean = when {
        name == PsiModifier.ABSTRACT && isImplementationInInterface() -> false
        // pretend this method behaves like a default method
        name == PsiModifier.DEFAULT && isImplementationInInterface() && !hasModifierProperty(PsiModifier.STATIC) -> true
        name == PsiModifier.FINAL &&
                (owner.containingClass.safeAs<KtLightClassForSourceDeclaration>()?.isPossiblyAffectedByAllOpen() == true)
        -> delegate.hasModifierProperty(name)

        else -> delegate.hasModifierProperty(name)
    }

    override fun hasExplicitModifier(name: String) =
        // Kotlin methods can't be truly default atm, that way we can avoid being reported on by diagnostics, namely UAST
        if (name == PsiModifier.DEFAULT) false else super.hasExplicitModifier(name)

    private fun isImplementationInInterface() = owner.containingClass.isInterface && owner.kotlinOrigin?.hasBody() == true

    override fun copy() = KtUltraLightMethodModifierList(support, owner, delegate)

    private inline fun <R> getTextVariantFromModifierListOfPropertyAccessorIfNeeded(
        retriever: (KtModifierList) -> R
    ): R? {
        val auxiliaryOrigin = (owner as? KtLightMember<*>)?.lightMemberOrigin?.auxiliaryOriginalElement
        return (auxiliaryOrigin as? KtPropertyAccessor)?.modifierList?.let(retriever)
    }

    override fun getText(): String {
        return getTextVariantFromModifierListOfPropertyAccessorIfNeeded(KtModifierList::getText)
            ?: super.getText()
    }

    override fun getTextOffset(): Int {
        return getTextVariantFromModifierListOfPropertyAccessorIfNeeded(KtModifierList::getTextOffset)
            ?: super.getTextOffset()
    }

    override fun getTextRange(): TextRange {
        return getTextVariantFromModifierListOfPropertyAccessorIfNeeded(KtModifierList::getTextRange)
            ?: super.getTextRange()
    }
}

internal abstract class KtUltraLightMethod(
    internal val delegate: PsiMethod,
    lightMemberOrigin: LightMemberOriginForDeclaration?,
    protected val support: KtUltraLightSupport,
    containingClass: KtLightClass,
    protected val methodIndex: Int
) : KtLightMethodImpl(
    lightMemberOrigin,
    containingClass
), KtUltraLightElementWithNullabilityAnnotationDescriptorBased<KtDeclaration, PsiMethod> {
    private class KtUltraLightThrowsReferenceListBuilder(private val parentMethod: PsiMethod) :
        KotlinLightReferenceListBuilder(parentMethod.manager, parentMethod.language, PsiReferenceList.Role.THROWS_LIST) {
        override fun getParent(): PsiMethod = parentMethod
        override fun getContainingFile(): PsiFile? = parentMethod.containingFile
    }

    protected fun computeThrowsList(methodDescriptor: FunctionDescriptor?): PsiReferenceList {
        val builder = KtUltraLightThrowsReferenceListBuilder(parentMethod = this)

        if (methodDescriptor !== null) {
            for (ex in FunctionCodegen.getThrownExceptions(methodDescriptor, LanguageVersionSettingsImpl.DEFAULT)) {
                val psiClassType = ex.defaultType.asPsiType(support, TypeMappingMode.DEFAULT, builder) as? PsiClassType
                psiClassType ?: continue
                builder.addReference(psiClassType)
            }
        }

        return builder
    }

    protected fun computeCheckNeedToErasureParametersTypes(methodDescriptor: FunctionDescriptor?): Boolean {

        if (methodDescriptor == null) return false

        val hasSpecialSignatureInfo = methodDescriptor.getSpecialSignatureInfo()
            ?.let { it.valueParametersSignature != null } ?: false
        if (hasSpecialSignatureInfo) return true

        // Workaround for KT-32245 that checks if this signature could be affected by KT-38406
        if (!DescriptorUtils.isOverride(methodDescriptor)) return false

        val hasStarProjectionParameterType = methodDescriptor.valueParameters
            .any { parameter -> parameter.type.arguments.any { it.isStarProjection } }
        if (!hasStarProjectionParameterType) return false

        return methodDescriptor.overriddenDescriptors
            .filterIsInstance<JavaMethodDescriptor>()
            .any { it.valueParameters.any { parameter -> parameter.type is RawType } }
    }

    abstract override fun buildTypeParameterList(): PsiTypeParameterList

    abstract val checkNeedToErasureParametersTypes: Boolean

    override val psiTypeForNullabilityAnnotation: PsiType? get() = returnType

    // These two overrides are necessary because ones from KtLightMethodImpl suppose that clsDelegate.returnTypeElement is valid
    // While here we only set return type for LightMethodBuilder (see org.jetbrains.kotlin.asJava.classes.KtUltraLightClass.asJavaMethod)
    override fun getReturnTypeElement(): PsiTypeElement? = null

    override fun getReturnType(): PsiType? = delegate.returnType

    override fun buildParametersForList(): List<PsiParameter> = delegate.parameterList.parameters.toList()

    private val _modifierList by lazyPub {
        KtUltraLightMethodModifierList(support, this, delegate)
    }

    override fun hasModifierProperty(name: String): Boolean = _modifierList.hasModifierProperty(name)
    override fun getModifierList(): PsiModifierList = _modifierList
    override fun getDefaultValue(): PsiAnnotationMemberValue? = delegate.safeAs<PsiAnnotationMethod>()?.defaultValue
    override fun getName(): String = delegate.name

    // should be in super
    override fun isVarArgs() = PsiImplUtil.isVarArgs(this)

    private val _deprecated: Boolean by lazyPub { kotlinOrigin?.isDeprecated(support) ?: false }

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

    override fun equals(other: Any?): Boolean = other === this ||
            other is KtUltraLightMethod &&
            other.methodIndex == methodIndex &&
            super.equals(other)

    override fun hashCode(): Int = super.hashCode().times(31).plus(methodIndex.hashCode())

    override fun isDeprecated(): Boolean = _deprecated

    override fun getDocComment(): PsiDocComment? = delegate.docComment

    override fun isConstructor(): Boolean = delegate.isConstructor
}

internal class KtUltraLightMethodForSourceDeclaration(
    delegate: PsiMethod,
    lightMemberOrigin: LightMemberOriginForDeclaration?,
    support: KtUltraLightSupport,
    containingClass: KtLightClass,
    private val forceToSkipNullabilityAnnotation: Boolean = false,
    methodIndex: Int
) : KtUltraLightMethod(
    delegate,
    lightMemberOrigin,
    support,
    containingClass,
    methodIndex
) {
    constructor(
        delegate: PsiMethod,
        declaration: KtDeclaration,
        support: KtUltraLightSupport,
        containingClass: KtLightClass,
        methodIndex: Int
    ) : this(
        delegate,
        LightMemberOriginForDeclaration(declaration, JvmDeclarationOriginKind.OTHER),
        support,
        containingClass,
        forceToSkipNullabilityAnnotation = false,
        methodIndex
    )

    override val qualifiedNameForNullabilityAnnotation: String?
        get() {
            val typeForAnnotation = if (forceToSkipNullabilityAnnotation) null else kotlinOrigin?.getKotlinType()
            return computeQualifiedNameForNullabilityAnnotation(typeForAnnotation)
        }

    override fun buildTypeParameterList(): PsiTypeParameterList {
        val origin = kotlinOrigin
        return if (origin is KtFunction || origin is KtProperty)
            buildTypeParameterListForSourceDeclaration(origin as KtTypeParameterListOwner, this, support)
        else LightTypeParameterListBuilder(manager, language)
    }

    private val methodDescriptor: FunctionDescriptor?
        get() {
            return when (val descriptor = kotlinOrigin?.resolve()) {
                is FunctionDescriptor -> descriptor
                is PropertyDescriptor -> {
                    when (methodIndex) {
                        METHOD_INDEX_FOR_GETTER -> descriptor.getter
                        METHOD_INDEX_FOR_SETTER -> descriptor.setter
                        else -> null
                    }
                }
                else -> null
            }
        }

    private val _throwsList: PsiReferenceList by lazyPub { computeThrowsList(methodDescriptor) }
    override fun getThrowsList(): PsiReferenceList = _throwsList

    override val checkNeedToErasureParametersTypes: Boolean by lazyPub { computeCheckNeedToErasureParametersTypes(methodDescriptor) }

    override fun equals(other: Any?): Boolean = other === this ||
            other is KtUltraLightMethodForSourceDeclaration &&
            other.forceToSkipNullabilityAnnotation == forceToSkipNullabilityAnnotation &&
            super.equals(other)

    override fun hashCode(): Int = super.hashCode() * 31 + forceToSkipNullabilityAnnotation.hashCode()
}

internal class KtUltraLightMethodForDescriptor(
    methodDescriptor: FunctionDescriptor,
    delegate: LightMethodBuilder,
    lightMemberOrigin: LightMemberOriginForDeclaration?,
    support: KtUltraLightSupport,
    containingClass: KtUltraLightClass
) : KtUltraLightMethod(
    delegate,
    lightMemberOrigin,
    support,
    containingClass,
    METHOD_INDEX_FOR_NON_ORIGIN_METHOD
) {
    // This is greedy realization of UL class.
    // This means that all data that depends on descriptor evaluated in ctor so the descriptor will be released on the end.
    // Be aware to save descriptor in class instance or any depending references

    private val lazyInitializers = mutableListOf<Lazy<*>>()
    private inline fun <T> getAndAddLazy(crossinline initializer: () -> T): Lazy<T> =
        lazyPub { initializer() }.also { lazyInitializers.add(it) }


    private val _buildTypeParameterList by getAndAddLazy {
        buildTypeParameterListForDescriptor(methodDescriptor, this, support)
    }

    override fun buildTypeParameterList() = _buildTypeParameterList

    private val _throwsList: PsiReferenceList by getAndAddLazy {
        computeThrowsList(methodDescriptor)
    }

    override fun getThrowsList(): PsiReferenceList = _throwsList

    override val givenAnnotations: List<KtLightAbstractAnnotation> by getAndAddLazy {
        methodDescriptor.obtainLightAnnotations(this)
    }

    override val qualifiedNameForNullabilityAnnotation: String? by getAndAddLazy {
        computeQualifiedNameForNullabilityAnnotation(methodDescriptor.returnType)
    }

    override val checkNeedToErasureParametersTypes: Boolean by getAndAddLazy {
        computeCheckNeedToErasureParametersTypes(methodDescriptor)
    }

    init {
        methodDescriptor.extensionReceiverParameter?.let { receiver ->
            delegate.addParameter(KtUltraLightParameterForDescriptor(receiver, support, this))
        }

        for (valueParameter in methodDescriptor.valueParameters) {
            delegate.addParameter(KtUltraLightParameterForDescriptor(valueParameter, support, this))
        }

        val returnType = if (methodDescriptor is ConstructorDescriptor) {
            delegate.isConstructor = true
            PsiType.VOID
        } else {
            support.mapType(methodDescriptor.returnType, this) { typeMapper, signatureWriter ->
                typeMapper.mapReturnType(methodDescriptor, signatureWriter)
            }
        }
        delegate.setMethodReturnType(returnType)

        //We should force computations on all lazy delegates to release descriptor on the end of ctor call
        with(lazyInitializers) {
            forEach { it.value }
            clear()
        }
    }
}