/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("FoldInitializerAndIfToElvis")

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.resolveFakeOverride
import org.jetbrains.kotlin.backend.konan.ir.DeserializerDriver
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

//-----------------------------------------------------------------------------//

internal class InlineConstructorsTransformation(val context: Context): IrElementTransformerVoidWithContext() {

    private val deserializer      by lazy { DeserializerDriver(context) }
    private val substituteMap     by lazy { mutableMapOf<ValueDescriptor, IrExpression>() }
    private val inlineConstructor by lazy { FqName("konan.internal.InlineConstructor") }

    //-------------------------------------------------------------------------//

    fun lower(irModule: IrModuleFragment) = irModule.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {

        val irCall = super.visitCall(expression) as IrCall
        val functionDescriptor = irCall.descriptor
        if (!functionDescriptor.annotations.hasAnnotation(inlineConstructor)) return irCall                                // This call does not need inlining.

        val functionDeclaration = getFunctionDeclaration(irCall)                            // Get declaration of the function to be inlined.
        if (functionDeclaration == null) {                                                  // We failed to get the declaration.
            val message = "Inliner failed to obtain function declaration: " +
                functionDescriptor.fqNameSafe.toString()
            context.reportWarning(message, currentFile, irCall)                             // Report warning.
            return irCall
        }

        val statements = (functionDeclaration.body as IrBlockBody).statements
        replaceDelegatingConstructorCall(statements, functionDescriptor)
        functionDeclaration.transformChildrenVoid(ValueSubstitutor())
        context.ir.originalModuleIndex.functions[functionDescriptor.original] = functionDeclaration

        return irCall
    }

    //-------------------------------------------------------------------------//

    private fun getFunctionDeclaration(irCall: IrCall): IrFunction? {

        val functionDescriptor = irCall.descriptor
        val originalDescriptor = functionDescriptor.resolveFakeOverride().original
        val functionDeclaration =
            context.ir.originalModuleIndex.functions[originalDescriptor] ?:                 // If function is declared in the current module.
                deserializer.deserializeInlineBody(originalDescriptor)                      // Function is declared in another module.
        return functionDeclaration as IrFunction?
    }

    //-------------------------------------------------------------------------//

    private fun replaceDelegatingConstructorCall(statements: MutableList<IrStatement>, functionDescriptor: FunctionDescriptor) {

        val delegatingCall = statements[0]
        if (delegatingCall !is IrDelegatingConstructorCallImpl) return

        val thisVariable = generateIrCall(delegatingCall)

        val newThisGetValue = IrGetValueImpl(                                                       // Create new expression, representing access the new variable.
            startOffset = 0,
            endOffset   = 0,
            descriptor  = thisVariable.descriptor
        )

        val classDescriptor = delegatingCall.descriptor.constructedClass
        val oldThisGetValue = classDescriptor.thisAsReceiverParameter
        substituteMap[oldThisGetValue] = newThisGetValue

        val newStatements = statements.toMutableList()
        newStatements[0] = thisVariable
        newStatements += newThisGetValue

        val block = IrBlockImpl(
            startOffset = 0,
            endOffset   = 0,
            type        = newThisGetValue.type,
            origin      = null,
            statements  = newStatements
        )

        val returnThis = IrReturnImpl(0, 0, functionDescriptor, block)

        statements.clear()
        statements += returnThis
    }

    //-------------------------------------------------------------------------//

    fun generateIrCall(expression: IrDelegatingConstructorCallImpl): IrVariable {

        val newExpression = IrCallImpl(
            expression.startOffset,
            expression.endOffset,
            expression.descriptor.returnType,
            expression.descriptor,
            expression.typeArguments,
            expression.origin
        ).apply {
            expression.descriptor.valueParameters.forEach {
                val valueArgument = expression.getValueArgument(it)
                putValueArgument(it.index, valueArgument)
            }
        }

        val newVariable = currentScope!!.scope.createTemporaryVariable(                      // Create new variable and init it with constructor call.
            irExpression = newExpression,
            nameHint     = newExpression.descriptor.fqNameSafe.toString() + ".this",
            isMutable    = false)

        return newVariable
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)

    //-------------------------------------------------------------------------//

    private inner class ValueSubstitutor: IrElementTransformerVoid() {

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val newExpression = super.visitGetValue(expression) as IrGetValue
            val descriptor = newExpression.descriptor
            val argument = substituteMap[descriptor]                                        // Find expression to replace this parameter.
            if (argument == null) return newExpression                                      // If there is no such expression - do nothing.
            return argument
        }

        //---------------------------------------------------------------------//

        override fun visitElement(element: IrElement) = element.accept(this, null)
    }
}