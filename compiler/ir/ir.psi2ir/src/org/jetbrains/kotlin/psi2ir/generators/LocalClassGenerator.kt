/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments

internal class LocalClassGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateObjectLiteral(ktObjectLiteral: KtObjectLiteralExpression): IrStatement {
        val startOffset = ktObjectLiteral.startOffsetSkippingComments
        val endOffset = ktObjectLiteral.endOffset
        val objectLiteralType = getTypeInferredByFrontendOrFail(ktObjectLiteral).toIrType()
        val irBlock = IrBlockImpl(startOffset, endOffset, objectLiteralType, IrStatementOrigin.OBJECT_LITERAL)

        val irClass = DeclarationGenerator(statementGenerator.context).generateClassOrObjectDeclaration(ktObjectLiteral.objectDeclaration)
        irBlock.statements.add(irClass)

        val objectConstructor = irClass.descriptor.unsubstitutedPrimaryConstructor
            ?: throw AssertionError("Object literal should have a primary constructor: ${irClass.descriptor}")
        assert(objectConstructor.dispatchReceiverParameter == null) {
            "Object literal constructor should have no dispatch receiver parameter: $objectConstructor"
        }
        assert(objectConstructor.extensionReceiverParameter == null) {
            "Object literal constructor should have no extension receiver parameter: $objectConstructor"
        }
        assert(objectConstructor.valueParameters.size == 0) {
            "Object literal constructor should have no value parameters: $objectConstructor"
        }

        irBlock.statements.add(
            IrConstructorCallImpl.fromSymbolDescriptor(
                startOffset, endOffset, objectLiteralType,
                context.symbolTable.descriptorExtension.referenceConstructor(objectConstructor),
                IrStatementOrigin.OBJECT_LITERAL
            )
        )

        return irBlock
    }

    fun generateLocalClass(ktClassOrObject: KtClassOrObject): IrStatement =
        DeclarationGenerator(statementGenerator.context).generateClassOrObjectDeclaration(ktClassOrObject)

}
