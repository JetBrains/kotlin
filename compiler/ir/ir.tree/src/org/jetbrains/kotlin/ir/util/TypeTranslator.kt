/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes

class TypeTranslator(
    moduleDescriptor: ModuleDescriptor,
    private val symbolTable: SymbolTable
) {

    private val annotationGenerator = AnnotationGenerator(moduleDescriptor, symbolTable)
    private val typeParametersResolver = ScopedTypeParametersResolver()

    fun enterScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.enterTypeParameterScope(irElement)
    }

    fun leaveScope() {
        typeParametersResolver.leaveTypeParameterScope()
    }

    private fun resolveTypeParameter(typeParameterDescriptor: TypeParameterDescriptor) =
        typeParametersResolver.resolveScopedTypeParameter(typeParameterDescriptor)
                ?: symbolTable.referenceTypeParameter(typeParameterDescriptor)

    fun translateType(ktType: KotlinType): IrType =
        translateType(ktType, Variance.INVARIANT).type

    private fun translateType(ktType0: KotlinType, variance: Variance): IrTypeProjection {
        // TODO "old" JVM BE does this for reified type arguments. Is it ok for arbitrary subexpressions?
        val ktTypeUpper = approximateCapturedTypes(ktType0).upper

        when {
            ktTypeUpper.isError -> return IrErrorTypeImpl(ktTypeUpper, translateTypeAnnotations(ktTypeUpper.annotations), variance)
            ktTypeUpper.isFlexible() -> return translateType(ktTypeUpper.upperIfFlexible(), variance)
            ktTypeUpper.isDynamic() -> return IrDynamicTypeImpl(ktTypeUpper, translateTypeAnnotations(ktTypeUpper.annotations), variance)
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

    private fun translateTypeAnnotations(annotations: Annotations): List<IrCall> =
        annotations.getAllAnnotations().map {
            // TODO filter out annotation targets
            annotationGenerator.generateAnnotationConstructorCall(it.annotation)
        }

    private fun translateTypeArguments(arguments: List<TypeProjection>) =
        arguments.map {
            // TODO starProjection
            translateType(it.type, it.projectionKind)
        }
}

