/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class AnnotationGenerator(context: GeneratorContext) : IrElementVisitorVoid {
    private val typeTranslator = context.typeTranslator
    private val constantValueGenerator = context.constantValueGenerator

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        if (declaration is IrTypeParametersContainer) {
            typeTranslator.enterScope(declaration)
        }
        generateAnnotationsForDeclaration(declaration)
        visitElement(declaration)
        if (declaration is IrTypeParametersContainer) {
            typeTranslator.leaveScope(declaration)
        }
    }

    private fun generateAnnotationsForDeclaration(declaration: IrDeclaration) {
        // Delegate field is mapped to a new property descriptor with annotations of the original property delegate
        // (see IrPropertyDelegateDescriptorImpl), but annotations on backing fields should be processed manually here
        val annotatedDescriptor =
            if (declaration is IrField && declaration.origin != IrDeclarationOrigin.DELEGATE)
                declaration.descriptor.backingField
            else declaration.descriptor

        annotatedDescriptor?.annotations?.mapTo(declaration.annotations) {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }
    }
}
