/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.transformations.AnnotationGenerator
import org.jetbrains.kotlin.psi2ir.transformations.ScopedTypeParametersResolver
import org.jetbrains.kotlin.types.*

class TypeTranslator(
    moduleDescriptor: ModuleDescriptor,
    private val symbolTable: SymbolTable
) {

    constructor(context: GeneratorContext) : this(context.moduleDescriptor, context.symbolTable)

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

    private fun translateType(ktType: KotlinType, variance: Variance): IrTypeProjection {
        when {
            ktType.isError -> return IrErrorTypeImpl(translateTypeAnnotations(ktType.annotations), variance)
            ktType.isFlexible() -> return translateType(ktType.upperIfFlexible(), variance)
            ktType.isDynamic() -> return IrDynamicTypeImpl(translateTypeAnnotations(ktType.annotations), variance)
        }

        val ktTypeConstructor = ktType.constructor
        val ktTypeDescriptor = ktTypeConstructor.declarationDescriptor ?: throw AssertionError("No descriptor for type $ktType")

        return when (ktTypeDescriptor) {
            is TypeParameterDescriptor ->
                IrSimpleTypeImpl(
                    resolveTypeParameter(ktTypeDescriptor),
                    ktType.isMarkedNullable,
                    emptyList(),
                    translateTypeAnnotations(ktType.annotations),
                    variance
                )

            is ClassDescriptor ->
                IrSimpleTypeImpl(
                    symbolTable.referenceClass(ktTypeDescriptor),
                    ktType.isMarkedNullable,
                    translateTypeArguments(ktType.arguments),
                    translateTypeAnnotations(ktType.annotations),
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

    private inner class SimpleTranslationVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitDeclaration(declaration: IrDeclaration) {
            if (declaration is IrTypeParametersContainer) {
                enterScope(declaration)
            }
            declaration.acceptChildrenVoid(this)
            if (declaration is IrTypeParametersContainer) {
                leaveScope()
            }
        }

        override fun visitExpression(expression: IrExpression) {
            translateType(expression.type)
            expression.acceptChildrenVoid(this)
        }
    }

    companion object {

        fun tryTranslateAllExpressionTypes(
            element: IrElement,
            context: GeneratorContext
        ) {
            element.acceptVoid(TypeTranslator(context).SimpleTranslationVisitor())
        }

    }
}

