/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.isKFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.descriptors.IrBasedTypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.threadLocal
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class TypeTranslator(
    protected val symbolTable: ReferenceSymbolTable,
    val languageVersionSettings: LanguageVersionSettings,
    typeParametersResolverBuilder: () -> TypeParametersResolver = { ScopedTypeParametersResolver() },
    private val enterTableScope: Boolean = false,
    protected val extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
) {
    abstract val constantValueGenerator: ConstantValueGenerator

    protected abstract fun approximateType(type: KotlinType): KotlinType

    protected abstract fun commonSupertype(types: Collection<KotlinType>): KotlinType

    private val typeParametersResolver by threadLocal { typeParametersResolverBuilder() }

    private val erasureStack = Stack<PropertyDescriptor>()

    private val supportDefinitelyNotNullTypes: Boolean = languageVersionSettings.supportsFeature(LanguageFeature.DefinitelyNonNullableTypes)

    protected abstract fun isTypeAliasAccessibleHere(typeAliasDescriptor: TypeAliasDescriptor): Boolean

    fun enterScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.enterTypeParameterScope(irElement)
        if (enterTableScope) {
            symbolTable.enterScope(irElement)
        }
    }

    fun leaveScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.leaveTypeParameterScope()
        if (enterTableScope) {
            symbolTable.leaveScope(irElement)
        }
    }

    fun <T> withTypeErasure(propertyDescriptor: PropertyDescriptor, b: () -> T): T {
        try {
            erasureStack.push(propertyDescriptor)
            return b()
        } finally {
            erasureStack.pop()
        }
    }

    fun <T> buildWithScope(container: IrTypeParametersContainer, builder: () -> T): T {
        enterScope(container)
        val result = builder()
        leaveScope(container)
        return result
    }

    private fun resolveTypeParameter(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameterSymbol {
        if (typeParameterDescriptor is IrBasedTypeParameterDescriptor) {
            return typeParameterDescriptor.owner.symbol
        }
        val originalTypeParameter = typeParameterDescriptor.originalTypeParameter
        return typeParametersResolver.resolveScopedTypeParameter(originalTypeParameter)
            ?: symbolTable.referenceTypeParameter(originalTypeParameter)
    }

    fun translateType(kotlinType: KotlinType): IrType {
        return translateType(kotlinType, Variance.INVARIANT).type
    }

    private fun translateType(kotlinType: KotlinType, variance: Variance): IrTypeProjection {
        val approximatedType = approximate(kotlinType.unwrap())

        when {
            approximatedType.isError ->
                return IrErrorTypeImpl(
                    approximatedType,
                    translateTypeAnnotations(approximatedType),
                    variance,
                    isMarkedNullable = approximatedType.isMarkedNullable,
                )
            approximatedType.isDynamic() ->
                return IrDynamicTypeImpl(approximatedType, translateTypeAnnotations(approximatedType), variance)
            supportDefinitelyNotNullTypes && approximatedType is DefinitelyNotNullType ->
                return makeTypeProjection(translateType(approximatedType.original).makeNotNull(), variance)
        }

        val upperType = approximatedType.upperIfFlexible()
        val upperTypeDescriptor = upperType.constructor.declarationDescriptor
            ?: throw AssertionError("No descriptor for type $upperType")

        if (erasureStack.isNotEmpty()) {
            if (upperTypeDescriptor is TypeParameterDescriptor) {
                if (upperTypeDescriptor.containingDeclaration in erasureStack) {
                    // This hack is about type parameter leak in case of generic delegated property
                    // Such code has to be prohibited since LV 1.5
                    // For more details see commit message or KT-24643
                    return approximateUpperBounds(upperTypeDescriptor.upperBounds, variance)
                }
            }
        }

        return IrSimpleTypeBuilder().apply {
            this.kotlinType = approximatedType
            this.nullability = SimpleTypeNullability.fromHasQuestionMark(upperType.isMarkedNullable)
            this.variance = variance
            this.abbreviation = upperType.getAbbreviation()?.toIrTypeAbbreviation()

            when (upperTypeDescriptor) {
                is TypeParameterDescriptor -> {
                    classifier = resolveTypeParameter(upperTypeDescriptor)
                    annotations = translateTypeAnnotations(upperType, approximatedType)
                }

                is ScriptDescriptor -> {
                    classifier = symbolTable.referenceScript(upperTypeDescriptor)
                }
                is ClassDescriptor -> {
                    // Types such as 'java.util.Collection<? extends CharSequence>' are treated as
                    // '( kotlin.collections.MutableCollection<out kotlin.CharSequence!>
                    //   .. kotlin.collections.Collection<kotlin.CharSequence!>? )'
                    // by the front-end.
                    // When generating generic signatures, JVM BE uses generic arguments of lower bound,
                    // thus producing 'java.util.Collection<? extends CharSequence>' from
                    // 'kotlin.collections.MutableCollection<out kotlin.CharSequence!>'.
                    // Construct equivalent type here.
                    // NB the difference is observed only when lowerTypeDescriptor != upperTypeDescriptor,
                    // which corresponds to mutability-flexible types such as mentioned above.
                    val lowerType = approximatedType.lowerIfFlexible()
                    val lowerTypeDescriptor =
                        lowerType.constructor.declarationDescriptor as? ClassDescriptor
                            ?: throw AssertionError("No class descriptor for lower type $lowerType of $approximatedType")
                    annotations = translateTypeAnnotations(upperType, approximatedType)
                    classifier = symbolTable.referenceClass(lowerTypeDescriptor)
                    arguments = when {
                        approximatedType is RawType ->
                            translateTypeArguments(approximatedType.arguments)
                        lowerTypeDescriptor != upperTypeDescriptor ->
                            translateTypeArguments(lowerType.arguments)
                        else ->
                            translateTypeArguments(upperType.arguments)
                    }

                }

                else ->
                    throw AssertionError("Unexpected type descriptor $upperTypeDescriptor :: ${upperTypeDescriptor::class}")
            }
        }.buildTypeProjection()
    }

    private fun approximateUpperBounds(upperBounds: Collection<KotlinType>, variance: Variance): IrTypeProjection {
        val commonSupertype = commonSupertype(upperBounds)
        return translateType(approximate(commonSupertype.replaceArgumentsWithStarProjections()), variance)
    }

    private fun SimpleType.toIrTypeAbbreviation(): IrTypeAbbreviation? {
        // Abbreviated type's classifier might not be TypeAliasDescriptor in case it's MockClassDescriptor (not found in dependencies).
        val typeAliasDescriptor = constructor.declarationDescriptor as? TypeAliasDescriptor ?: return null

        // There is possible situation when we have private top-level type alias visible outside its file which is illegal from klib POV.
        // In that specific case don't generate type abbreviation
        if (!isTypeAliasAccessibleHere(typeAliasDescriptor)) return null

        return IrTypeAbbreviationImpl(
            symbolTable.referenceTypeAlias(typeAliasDescriptor),
            isMarkedNullable,
            translateTypeArguments(this.arguments),
            translateTypeAnnotations(this)
        )
    }

    fun approximate(ktType: KotlinType): KotlinType {
        val properlyApproximatedType = approximateByKotlinRules(ktType)

        // If there's an intersection type, take the most common supertype of its intermediate supertypes.
        // That's what old back-end effectively does.
        val typeConstructor = properlyApproximatedType.constructor
        if (typeConstructor is IntersectionTypeConstructor) {
            val commonSupertype = commonSupertype(typeConstructor.supertypes)
            return approximate(commonSupertype.replaceArgumentsWithStarProjections())
        }

        // Assume that other types are approximated properly.
        return properlyApproximatedType
    }

    fun approximateFunctionReferenceType(kotlinType: KotlinType): KotlinType {
        // This is a hack to support intersection types in function references on JVM.
        // Function reference type KFunctionN<T1, ..., TN, R> might contain intersection types in its top-level arguments.
        // Intersection types in expressions and local variable declarations usually don't bother us.
        // However, in case of function references type mapping affects behavior:
        // resulting function reference class will have a bridge method, which will downcast its arguments to the expected types.
        // This would cause ClassCastException in case of usual type approximation,
        // because '{ X1 & ... & Xm }' would be approximated to 'Nothing'.
        // JVM_OLD just relies on type mapping for generic argument types in such case.
        if (!kotlinType.isKFunctionType)
            return kotlinType
        if (kotlinType !is SimpleType)
            return kotlinType
        if (kotlinType.arguments.none { it.type.constructor is IntersectionTypeConstructor })
            return kotlinType
        val functionParameterTypes = kotlinType.arguments.subList(0, kotlinType.arguments.size - 1)
        val functionReturnType = kotlinType.arguments.last()
        return kotlinType.replace(
            newArguments = functionParameterTypes.map { approximateFunctionReferenceParameterType(it) } + functionReturnType
        )
    }

    private fun approximateFunctionReferenceParameterType(typeProjection: TypeProjection): TypeProjection {
        if (typeProjection.isStarProjection) return typeProjection
        val typeConstructor = typeProjection.type.constructor as? IntersectionTypeConstructor
            ?: return typeProjection
        // 'mapType' takes common supertype for intersection type supertypes, regardless of variance.
        val newType = typeConstructor.getAlternativeType()
            ?: commonSupertype(typeConstructor.supertypes)
        return TypeProjectionImpl(typeProjection.projectionKind, newType)
    }

    private val isWithNewInference = languageVersionSettings.supportsFeature(LanguageFeature.NewInference)

    private fun approximateByKotlinRules(ktType: KotlinType): KotlinType =
        if (isWithNewInference) {
            if (ktType.constructor.isDenotable && ktType.arguments.isEmpty())
                ktType
            else
                approximateType(ktType)
        } else {
            // Hack to preserve *-projections in arguments in OI.
            // Expected to be removed as soon as OI is deprecated.
            if (ktType.constructor.isDenotable)
                ktType
            else
                approximateCapturedTypes(ktType).upper
        }

    private fun translateTypeAnnotations(kotlinType: KotlinType, flexibleType: KotlinType = kotlinType): List<IrConstructorCall> {
        val annotations = kotlinType.annotations
        val irAnnotations = ArrayList<IrConstructorCall>()

        annotations.mapNotNullTo(irAnnotations) {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }

        // EnhancedNullability annotation is not present in 'annotations', see 'EnhancedTypeAnnotations::iterator()'.
        // Also, EnhancedTypeAnnotationDescriptor is not a "real" annotation descriptor, there's no corresponding ClassDescriptor, etc.
        if (extensions.enhancedNullability.hasEnhancedNullability(kotlinType)) {
            irAnnotations.addSpecialAnnotation(extensions.enhancedNullabilityAnnotationConstructor)
        }

        if (flexibleType.isNullabilityFlexible()) {
            irAnnotations.addSpecialAnnotation(extensions.flexibleNullabilityAnnotationConstructor)
        }
        if (flexibleType.isMutabilityFlexible()) {
            irAnnotations.addSpecialAnnotation(extensions.flexibleMutabilityAnnotationConstructor)
        }

        if (flexibleType is RawType) {
            irAnnotations.addSpecialAnnotation(extensions.rawTypeAnnotationConstructor)
        }

        return irAnnotations
    }

    private fun KotlinType.isMutabilityFlexible(): Boolean {
        val flexibility = unwrap()
        return flexibility is FlexibleType && flexibility.lowerBound.constructor != flexibility.upperBound.constructor &&
                FlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(flexibility.lowerBound) ==
                FlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(flexibility.upperBound)
    }

    private fun MutableList<IrConstructorCall>.addSpecialAnnotation(irConstructor: IrConstructor?) {
        if (irConstructor != null) {
            add(
                IrConstructorCallImpl.fromSymbolOwner(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    irConstructor.constructedClassType,
                    irConstructor.symbol
                )
            )
        }
    }

    private fun translateTypeArguments(arguments: List<TypeProjection>) =
        arguments.memoryOptimizedMap {
            if (it.isStarProjection)
                IrStarProjectionImpl
            else
                translateType(it.type, it.projectionKind)
        }
}
