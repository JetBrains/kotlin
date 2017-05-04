/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS

class ReflectionReferencesGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateClassLiteral(ktClassLiteral: KtClassLiteralExpression): IrExpression {
        val ktArgument = ktClassLiteral.receiverExpression!!
        val lhs = getOrFail(BindingContext.DOUBLE_COLON_LHS, ktArgument)
        val resultType = getInferredTypeWithImplicitCastsOrFail(ktClassLiteral)

        return if (lhs is DoubleColonLHS.Expression && !lhs.isObjectQualifier) {
            IrGetClassImpl(ktClassLiteral.startOffset, ktClassLiteral.endOffset, resultType,
                           statementGenerator.generateExpression(ktArgument))
        }
        else {
            val typeConstructorDeclaration = lhs.type.constructor.declarationDescriptor
            val typeClass = typeConstructorDeclaration ?:
                            throw AssertionError("Unexpected type constructor for ${lhs.type}: $typeConstructorDeclaration")
            IrClassReferenceImpl(ktClassLiteral.startOffset, ktClassLiteral.endOffset, resultType,
                                 context.symbolTable.referenceClassifier(typeClass))
        }
    }

    fun generateCallableReference(ktCallableReference: KtCallableReferenceExpression): IrExpression {
        val resolvedCall = getResolvedCall(ktCallableReference.callableReference)!!
        val irCallableRef = generateCallableReference(
                ktCallableReference.startOffset, ktCallableReference.endOffset,
                getInferredTypeWithImplicitCastsOrFail(ktCallableReference),
                resolvedCall.resultingDescriptor,
                typeArguments = null
        )
        resolvedCall.dispatchReceiver?.let { dispatchReceiver ->
            if (dispatchReceiver !is TransientReceiver) {
                irCallableRef.dispatchReceiver = statementGenerator.generateReceiver(ktCallableReference, dispatchReceiver).load()
            }
        }
        resolvedCall.extensionReceiver?.let { extensionReceiver ->
            if (extensionReceiver !is TransientReceiver) {
                irCallableRef.extensionReceiver = statementGenerator.generateReceiver(ktCallableReference, extensionReceiver).load()
            }
        }

        return irCallableRef
    }

    fun generateCallableReference(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType,
            callableDescriptor: CallableDescriptor,
            typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
            origin: IrStatementOrigin? = null
    ): IrCallableReference =
            when (callableDescriptor) {
                is FunctionDescriptor ->
                    generateFunctionReference(
                            startOffset, endOffset, type,
                            context.symbolTable.referenceFunction(callableDescriptor.original),
                            callableDescriptor,
                            typeArguments,
                            origin
                    )
                is PropertyDescriptor ->
                    generatePropertyReference(startOffset, endOffset, type, callableDescriptor, typeArguments, origin)
                else ->
                    throw AssertionError("Unexpected callable reference: $callableDescriptor")
            }

    fun generateLocalDelegatedPropertyReference(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType,
            variableDescriptor: VariableDescriptorWithAccessors,
            irDelegateSymbol: IrVariableSymbol,
            origin: IrStatementOrigin?
    ): IrLocalDelegatedPropertyReference {
        val getterDescriptor = variableDescriptor.getter ?:
                               throw AssertionError("Local delegated property should have a getter: $variableDescriptor")
        val setterDescriptor = variableDescriptor.setter

        val getterSymbol = context.symbolTable.referenceFunction(getterDescriptor)
        val setterSymbol = setterDescriptor?.let { context.symbolTable.referenceFunction(it) }

        return IrLocalDelegatedPropertyReferenceImpl(
                startOffset, endOffset, type,
                variableDescriptor,
                irDelegateSymbol, getterSymbol, setterSymbol,
                origin
        )
    }

    private fun generatePropertyReference(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType,
            propertyDescriptor: PropertyDescriptor,
            typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
            origin: IrStatementOrigin?
    ): IrPropertyReference {
        val getterDescriptor = propertyDescriptor.getter
        val setterDescriptor = propertyDescriptor.setter

        val fieldSymbol = if (getterDescriptor == null) context.symbolTable.referenceField(propertyDescriptor) else null
        val getterSymbol = getterDescriptor?.let { context.symbolTable.referenceFunction(it.original) }
        val setterSymbol = setterDescriptor?.let { context.symbolTable.referenceFunction(it.original) }

        return IrPropertyReferenceImpl(
                startOffset, endOffset, type,
                propertyDescriptor,
                fieldSymbol, getterSymbol, setterSymbol,
                typeArguments,
                origin
        )
    }

    fun generateFunctionReference(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType,
            symbol: IrFunctionSymbol,
            descriptor: FunctionDescriptor,
            typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
            origin: IrStatementOrigin?
    ): IrFunctionReference =
            IrFunctionReferenceImpl(
                    startOffset, endOffset, type,
                    symbol, descriptor,
                    typeArguments,
                    origin
            )
}