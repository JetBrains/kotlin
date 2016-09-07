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

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCallableReferenceImpl
import org.jetbrains.kotlin.ir.expressions.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetClassImpl
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS

class ReflectionReferencesGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateClassLiteral(ktClassLiteral: KtClassLiteralExpression): IrExpression {
        val ktArgument = ktClassLiteral.receiverExpression!!
        val lhs = getOrFail(BindingContext.DOUBLE_COLON_LHS, ktArgument)
        val resultType = getInferredTypeWithImplicitCastsOrFail(ktClassLiteral)

        return if (lhs is DoubleColonLHS.Expression && !lhs.isObject) {
            IrGetClassImpl(ktClassLiteral.startOffset, ktClassLiteral.endOffset, resultType,
                           statementGenerator.generateExpression(ktArgument))
        }
        else {
            val typeConstructorDeclaration = lhs.type.constructor.declarationDescriptor
            val typeClass = typeConstructorDeclaration as? ClassifierDescriptor ?:
                            throw AssertionError("Unexpected type constructor for ${lhs.type}: $typeConstructorDeclaration")
            IrClassReferenceImpl(ktClassLiteral.startOffset, ktClassLiteral.endOffset, resultType, typeClass)
        }
    }

    fun generateCallableReference(ktCallableReference: KtCallableReferenceExpression): IrExpression {
        val resolvedCall = getResolvedCall(ktCallableReference.callableReference)!!
        val irCallableRef = IrCallableReferenceImpl(ktCallableReference.startOffset, ktCallableReference.endOffset,
                                                    getInferredTypeWithImplicitCastsOrFail(ktCallableReference),
                                                    resolvedCall.resultingDescriptor)
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


}