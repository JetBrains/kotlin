/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes

class TypeTranslator(
    private val symbolTable: ReferenceSymbolTable,
    val languageVersionSettings: LanguageVersionSettings,
    private val typeParametersResolver: TypeParametersResolver = ScopedTypeParametersResolver(),
    private val enterTableScope: Boolean = false
) {

    private val typeApproximatorForNI = TypeApproximator()
    lateinit var constantValueGenerator: ConstantValueGenerator

    fun enterScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.enterTypeParameterScope(irElement)
        if(enterTableScope) {
            symbolTable.enterScope(irElement.descriptor)
        }
    }

    fun leaveScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.leaveTypeParameterScope()
        if(enterTableScope) {
            symbolTable.leaveScope(irElement.descriptor)
        }
    }

    inline fun <T> buildWithScope(container: IrTypeParametersContainer, builder: () -> T): T {
        enterScope(container)
        val result = builder()
        leaveScope(container)
        return result
    }

    private fun resolveTypeParameter(typeParameterDescriptor: TypeParameterDescriptor) =
        typeParametersResolver.resolveScopedTypeParameter(typeParameterDescriptor)
                ?: symbolTable.referenceTypeParameter(typeParameterDescriptor)

    fun translateType(ktType: KotlinType): IrType =
        translateType(ktType, Variance.INVARIANT).type

    private fun translateType(ktType0: KotlinType, variance: Variance): IrTypeProjection {
        // TODO "old" JVM BE does this for reified type arguments. Is it ok for arbitrary subexpressions?

        val ktTypeUpper = ktType0.approximate(languageVersionSettings)

        when {
            ktTypeUpper.isError -> return IrErrorTypeImpl(ktTypeUpper, translateTypeAnnotations(ktTypeUpper.annotations), variance)
            ktTypeUpper.isDynamic() -> return IrDynamicTypeImpl(ktTypeUpper, translateTypeAnnotations(ktTypeUpper.annotations), variance)
            ktTypeUpper.isFlexible() -> return translateType(ktTypeUpper.upperIfFlexible(), variance)
        }

        val ktTypeConstructor = ktTypeUpper.constructor
        val ktTypeDescriptor = ktTypeConstructor.declarationDescriptor ?: throw AssertionError("No descriptor for type $ktTypeUpper")

        return when (ktTypeDescriptor) {
            is TypeParameterDescriptor ->
                IrSimpleTypeImpl(
                    ktTypeUpper,
                    resolveTypeParameter(ktTypeDescriptor),
                    ktTypeUpper.isMarkedNullable,
                    emptyList(),
                    translateTypeAnnotations(ktTypeUpper.annotations),
                    variance
                )

            is ClassDescriptor ->
                IrSimpleTypeImpl(
                    ktTypeUpper,
                    symbolTable.referenceClass(ktTypeDescriptor),
                    ktTypeUpper.isMarkedNullable,
                    translateTypeArguments(ktTypeUpper.arguments),
                    translateTypeAnnotations(ktTypeUpper.annotations),
                    variance
                )

            else ->
                throw AssertionError("Unexpected type descriptor $ktTypeDescriptor :: ${ktTypeDescriptor::class}")
        }
    }

    private fun KotlinType.approximate(languageVersionSettings: LanguageVersionSettings): KotlinType {
        if (this.constructor.isDenotable) return this

        return if (languageVersionSettings.supportsFeature(LanguageFeature.NewInference))
            typeApproximatorForNI.approximateDeclarationType(this, local = false, languageVersionSettings = languageVersionSettings)
        else
            approximateCapturedTypes(this).upper
    }

    private fun translateTypeAnnotations(annotations: Annotations): List<IrCall> =
        annotations.map(constantValueGenerator::generateAnnotationConstructorCall)

    private fun translateTypeArguments(arguments: List<TypeProjection>) =
        arguments.map {
            if (it.isStarProjection)
                IrStarProjectionImpl
            else
                translateType(it.type, it.projectionKind)
        }
}
