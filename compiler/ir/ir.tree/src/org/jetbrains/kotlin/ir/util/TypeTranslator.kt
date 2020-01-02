/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
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

class TypeTranslator(
    private val symbolTable: ReferenceSymbolTable,
    val languageVersionSettings: LanguageVersionSettings,
    builtIns: KotlinBuiltIns,
    private val typeParametersResolver: TypeParametersResolver = ScopedTypeParametersResolver(),
    private val enterTableScope: Boolean = false
) {

    private val typeApproximatorForNI = TypeApproximator(builtIns)
    lateinit var constantValueGenerator: ConstantValueGenerator

    fun enterScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.enterTypeParameterScope(irElement)
        if (enterTableScope) {
            symbolTable.enterScope(irElement.descriptor)
        }
    }

    fun leaveScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.leaveTypeParameterScope()
        if (enterTableScope) {
            symbolTable.leaveScope(irElement.descriptor)
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
        translateType(kotlinType, kotlinType, Variance.INVARIANT).type

    private fun translateType(kotlinType: KotlinType, approximatedKotlinType: KotlinType, variance: Variance): IrTypeProjection {
        val approximatedType = LegacyTypeApproximation().approximate(kotlinType)

        when {
            approximatedType.isError ->
                return IrErrorTypeImpl(approximatedKotlinType, translateTypeAnnotations(approximatedType.annotations), variance)
            approximatedType.isDynamic() ->
                return IrDynamicTypeImpl(approximatedKotlinType, translateTypeAnnotations(approximatedType.annotations), variance)
            approximatedType.isFlexible() ->
                return translateType(approximatedType.upperIfFlexible(), approximatedType, variance)
        }

        val ktTypeConstructor = approximatedType.constructor
        val ktTypeDescriptor = ktTypeConstructor.declarationDescriptor
            ?: throw AssertionError("No descriptor for type $approximatedType")

        return IrSimpleTypeBuilder().apply {
            this.kotlinType = approximatedKotlinType
            hasQuestionMark = approximatedType.isMarkedNullable
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

    private inner class LegacyTypeApproximation {

        fun approximate(ktType: KotlinType): KotlinType {
            val properlyApproximatedType = approximateByKotlinRules(ktType)

            // If there's an intersection type, take the most common supertype of its intermediate supertypes.
            // That's what old back-end effectively does.
            val typeConstructor = properlyApproximatedType.constructor
            if (typeConstructor is IntersectionTypeConstructor) {
                val commonSupertype = CommonSupertypes.commonSupertype(typeConstructor.supertypes)
                return approximate(commonSupertype.replaceArgumentsWithStarProjections())
            }

            // Other types should be approximated properly. Right? Riiight?
            return properlyApproximatedType
        }


        private fun approximateByKotlinRules(ktType: KotlinType): KotlinType {
            if (ktType.constructor.isDenotable) return ktType

            return if (languageVersionSettings.supportsFeature(LanguageFeature.NewInference))
                typeApproximatorForNI.approximateDeclarationType(
                    ktType,
                    local = false,
                    languageVersionSettings = languageVersionSettings
                )
            else
                approximateCapturedTypes(ktType).upper
        }

    }

    private fun translateTypeAnnotations(annotations: Annotations): List<IrConstructorCall> =
        annotations.mapNotNull(constantValueGenerator::generateAnnotationConstructorCall)

    private fun translateTypeArguments(arguments: List<TypeProjection>) =
        arguments.map {
            if (it.isStarProjection)
                IrStarProjectionImpl
            else
                translateType(it.type, it.type, it.projectionKind)
        }
}
