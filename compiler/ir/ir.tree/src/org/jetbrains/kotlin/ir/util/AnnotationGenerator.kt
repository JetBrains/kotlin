/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.types.KotlinType

class AnnotationGenerator(
    moduleDescriptor: ModuleDescriptor,
    symbolTable: SymbolTable
) : IrElementVisitorVoid {

    private val typeTranslator = TypeTranslator(moduleDescriptor, symbolTable)
    private val scopedTypeParameterResolver = ScopedTypeParametersResolver()
    private val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable, scopedTypeParameterResolver)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        if (declaration is IrTypeParametersContainer) {
            scopedTypeParameterResolver.enterTypeParameterScope(declaration)
        }
        generateAnnotationsForDeclaration(declaration)
        visitElement(declaration)
        if (declaration is IrTypeParametersContainer) {
            scopedTypeParameterResolver.leaveTypeParameterScope()
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        super.visitValueParameter(declaration)

        val descriptor = declaration.descriptor
        val containingDeclaration = descriptor.containingDeclaration

        if (containingDeclaration is PropertySetterDescriptor) {
            containingDeclaration.correspondingProperty.annotations.getUseSiteTargetedAnnotations()
                .filter { it.target == AnnotationUseSiteTarget.SETTER_PARAMETER }
                .generateAnnotationConstructorCalls(declaration)
        }

        descriptor.type.annotations.getAllAnnotations()
            .filter { it.target == AnnotationUseSiteTarget.RECEIVER }
            .generateAnnotationConstructorCalls(declaration)
    }

    private fun generateAnnotationsForDeclaration(declaration: IrDeclaration) {
        declaration.descriptor.annotations.getAllAnnotations()
            .filter { isAnnotationTargetMatchingDeclaration(it.target, declaration) }
            .generateAnnotationConstructorCalls(declaration)
    }

    private fun List<AnnotationWithTarget>.generateAnnotationConstructorCalls(declaration: IrDeclaration) {
        mapTo(declaration.annotations) {
            constantValueGenerator.generateAnnotationConstructorCall(it.annotation)
        }
    }

    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    private fun isAnnotationTargetMatchingDeclaration(target: AnnotationUseSiteTarget?, element: IrElement): Boolean =
        when (element) {
            is IrProperty ->
                target == null || target == AnnotationUseSiteTarget.PROPERTY

            is IrField ->
                target == AnnotationUseSiteTarget.FIELD || target == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD

            is IrSimpleFunction ->
                target == null || target == AnnotationUseSiteTarget.PROPERTY_GETTER || target == AnnotationUseSiteTarget.PROPERTY_SETTER

            is IrValueParameter ->
                target == null || target == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER

            else -> target == null
        }
}


