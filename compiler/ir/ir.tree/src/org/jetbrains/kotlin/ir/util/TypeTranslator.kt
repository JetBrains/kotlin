/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import java.util.*

@OptIn(DescriptorBasedIr::class)
class TypeTranslator(
    private val symbolTable: ReferenceSymbolTable,
    val languageVersionSettings: LanguageVersionSettings,
    builtIns: KotlinBuiltIns,
    private val typeParametersResolver: TypeParametersResolver = ScopedTypeParametersResolver(),
    private val enterTableScope: Boolean = false
) {

    private val erasureStack = Stack<PropertyDescriptor>()

    private val typeApproximatorForNI = TypeApproximator(builtIns)
    lateinit var constantValueGenerator: ConstantValueGenerator

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

    inline fun <T> buildWithScope(container: IrTypeParametersContainer, builder: () -> T): T {
        enterScope(container)
        val result = builder()
        leaveScope(container)
        return result
    }

    private fun resolveTypeParameter(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameterSymbol {
        val originalTypeParameter = typeParameterDescriptor.originalTypeParameter
        return typeParametersResolver.resolveScopedTypeParameter(originalTypeParameter)
            ?: symbolTable.referenceTypeParameter(originalTypeParameter)
    }

    fun translateType(kotlinType: KotlinType): IrType =
        translateType(kotlinType, Variance.INVARIANT).type

    private fun translateType(kotlinType: KotlinType, variance: Variance): IrTypeProjection {
        val flexibleApproximatedType = approximate(kotlinType)

        when {
            flexibleApproximatedType.isError ->
                return IrErrorTypeImpl(flexibleApproximatedType, translateTypeAnnotations(flexibleApproximatedType.annotations), variance)
            flexibleApproximatedType.isDynamic() ->
                return IrDynamicTypeImpl(flexibleApproximatedType, translateTypeAnnotations(flexibleApproximatedType.annotations), variance)
        }

        val approximatedType = flexibleApproximatedType.upperIfFlexible()

        val ktTypeConstructor = approximatedType.constructor
        val ktTypeDescriptor = ktTypeConstructor.declarationDescriptor
            ?: throw AssertionError("No descriptor for type $approximatedType")

        if (erasureStack.isNotEmpty()) {
            if (ktTypeDescriptor is TypeParameterDescriptor) {
                if (ktTypeDescriptor.containingDeclaration in erasureStack) {
                    // This hack is about type parameter leak in case of generic delegated property
                    // Such code has to be prohibited since LV 1.5
                    // For more details see commit message or KT-24643
                    return approximateUpperBounds(ktTypeDescriptor.upperBounds, variance)
                }
            }
        }

        return IrSimpleTypeBuilder().apply {
            this.kotlinType = flexibleApproximatedType
            this.hasQuestionMark = approximatedType.isMarkedNullable
            this.variance = variance
            this.abbreviation = approximatedType.getAbbreviation()?.toIrTypeAbbreviation()
            when (ktTypeDescriptor) {
                is TypeParameterDescriptor -> {
                    classifier = resolveTypeParameter(ktTypeDescriptor)
                    annotations = translateTypeAnnotations(approximatedType.annotations)
                }

                is ClassDescriptor -> {
                    classifier = symbolTable.referenceClass(ktTypeDescriptor)
                    arguments = translateTypeArguments(approximatedType.arguments)
                    annotations = translateTypeAnnotations(approximatedType.annotations)
                }

                else ->
                    throw AssertionError("Unexpected type descriptor $ktTypeDescriptor :: ${ktTypeDescriptor::class}")
            }
        }.buildTypeProjection()
    }

    private fun approximateUpperBounds(upperBounds: Collection<KotlinType>, variance: Variance): IrTypeProjection {
        val commonSupertype = CommonSupertypes.commonSupertype(upperBounds)
        return translateType(approximate(commonSupertype.replaceArgumentsWithStarProjections()), variance)
    }

    private fun SimpleType.toIrTypeAbbreviation(): IrTypeAbbreviation {
        val typeAliasDescriptor = constructor.declarationDescriptor.let {
            it as? TypeAliasDescriptor
                ?: throw AssertionError("TypeAliasDescriptor expected: $it")
        }
        return IrTypeAbbreviationImpl(
            symbolTable.referenceTypeAlias(typeAliasDescriptor),
            isMarkedNullable,
            translateTypeArguments(this.arguments),
            translateTypeAnnotations(this.annotations)
        )
    }

    fun approximate(ktType: KotlinType): KotlinType {
        val properlyApproximatedType = approximateByKotlinRules(ktType)

        // If there's an intersection type, take the most common supertype of its intermediate supertypes.
        // That's what old back-end effectively does.
        val typeConstructor = properlyApproximatedType.constructor
        if (typeConstructor is IntersectionTypeConstructor) {
            val commonSupertype = CommonSupertypes.commonSupertype(typeConstructor.supertypes)
            return approximate(commonSupertype.replaceArgumentsWithStarProjections())
        }

        // Assume that other types are approximated properly.
        return properlyApproximatedType
    }

    private val isWithNewInference = languageVersionSettings.supportsFeature(LanguageFeature.NewInference)

    private fun approximateByKotlinRules(ktType: KotlinType): KotlinType =
        if (isWithNewInference) {
            if (ktType.constructor.isDenotable && ktType.arguments.isEmpty())
                ktType
            else
                typeApproximatorForNI.approximateDeclarationType(
                    ktType,
                    local = false,
                    languageVersionSettings = languageVersionSettings
                )
        } else {
            // Hack to preserve *-projections in arguments in OI.
            // Expected to be removed as soon as OI is deprecated.
            if (ktType.constructor.isDenotable)
                ktType
            else
                approximateCapturedTypes(ktType).upper
        }

    private fun translateTypeAnnotations(annotations: Annotations): List<IrConstructorCall> =
        annotations.mapNotNull(constantValueGenerator::generateAnnotationConstructorCall)

    private fun translateTypeArguments(arguments: List<TypeProjection>) =
        arguments.map {
            if (it.isStarProjection)
                IrStarProjectionImpl
            else
                translateType(it.type, it.projectionKind)
        }
}
