/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.assertCast
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.indexOrMinusOne
import org.jetbrains.kotlin.ir.util.isCrossinline
import org.jetbrains.kotlin.ir.util.isNoinline
import org.jetbrains.kotlin.ir.util.varargElementType
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.psi2ir.intermediate.createTemporaryVariableInBlock
import org.jetbrains.kotlin.psi2ir.intermediate.setExplicitReceiverValue
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore

class ScriptGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {
    fun generateScriptDeclaration(ktScript: KtScript): IrDeclaration? {
        val descriptor = getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktScript) as ScriptDescriptor

        val existedScripts = context.symbolTable.listExistedScripts()

        return context.symbolTable.declareScript(descriptor).buildWithScope { irScript ->

            // A workaround for the JS/REPL backend:
            // JS backend doesn't save previously executed snippets anywhere, they should be taken for now from the context.symbolTable.listExistedScripts()
            // on the other hand imported scripts are also stored there so to avoid clashes the imported scripts should be filtered out
            // NOTE: that JVM IR is not properly tested with REPL, so it might have the same problem
            // TODO: design and implement other schema for handling previous snippets
            val importedScripts = descriptor.implicitReceivers.filterIsInstanceTo(HashSet<ScriptDescriptor>())

            // TODO: since script could reference instances of previous one their receivers have to be enlisted in its scope
            // Remove this code once script is no longer represented by Class
            existedScripts.forEach {
                if (it.owner != irScript && it.descriptor !in importedScripts) {
                    context.symbolTable.introduceValueParameter(it.owner.thisReceiver)
                }
            }

            val startOffset = ktScript.pureStartOffset
            val endOffset = ktScript.pureEndOffset

            fun makeParameter(descriptor: ParameterDescriptor, origin: IrDeclarationOrigin, index: Int = -1): IrValueParameter {
                val type = descriptor.type.toIrType()
                val varargElementType = descriptor.varargElementType?.toIrType()
                return context.symbolTable.declareValueParameter(
                    startOffset, endOffset,
                    origin,
                    descriptor,
                    type
                ) { symbol ->
                    context.irFactory.createValueParameter(
                        startOffset, endOffset,
                        origin, symbol, context.symbolTable.nameProvider.nameForDeclaration(descriptor),
                        if (index != -1) index else descriptor.indexOrMinusOne,
                        type, varargElementType,
                        descriptor.isCrossinline, descriptor.isNoinline, false
                    )
                } .also { it.parent = irScript }
            }

            irScript.thisReceiver = makeParameter(descriptor.thisAsReceiverParameter, IrDeclarationOrigin.INSTANCE_RECEIVER)

            irScript.baseClass = descriptor.typeConstructor.supertypes.single().toIrType()

            // This is part of a hack for implicit receivers that converted to value parameters below
            // The proper schema would be to get properly indexed parameters from frontend (descriptor.implicitReceiversParameters),
            // but it seems would require a proper remapping for the script body
            // TODO: implement implicit receiver parameters handlin properly
            var parametersIndex = 0

            irScript.explicitCallParameters = descriptor.explicitConstructorParameters.map { valueParameterDescriptor ->
                parametersIndex++
                valueParameterDescriptor.toIrValueParameter(startOffset, endOffset, IrDeclarationOrigin.SCRIPT_CALL_PARAMETER).also { it.parent = irScript }
            }

            irScript.implicitReceiversParameters = descriptor.implicitReceivers.map {
                makeParameter(it.thisAsReceiverParameter, IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER, parametersIndex++)
            }

            irScript.providedProperties = descriptor.scriptProvidedProperties.zip(descriptor.scriptProvidedPropertiesParameters)
                .map { (providedProperty, parameter) ->
                    // TODO: initializer
                    // TODO: do not keep direct links
                    val type = providedProperty.type.toIrType()
                    val valueParameter = context.symbolTable.declareValueParameter(
                        startOffset, endOffset, IrDeclarationOrigin.SCRIPT_PROVIDED_PROPERTY, parameter, type
                    )
                    val irProperty =
                        PropertyGenerator(declarationGenerator).generateSyntheticProperty(ktScript, providedProperty, valueParameter)
                    irProperty.origin = IrDeclarationOrigin.SCRIPT_PROVIDED_PROPERTY
                    irScript.statements += irProperty
                    valueParameter to irProperty.symbol
                }

            irScript.earlierScripts = existedScripts

            for (d in ktScript.declarations) {
                when (d) {
                    is KtScriptInitializer -> {
                        val irExpressionBody = BodyGenerator(
                            irScript.symbol,
                            context
                        ).generateExpressionBody(d.body!!)
                        if (d == ktScript.declarations.last() && descriptor.resultValue != null) {
                            descriptor.resultValue!!.let { resultDescriptor ->
                                PropertyGenerator(declarationGenerator)
                                    .generateSyntheticPropertyWithInitializer(ktScript, resultDescriptor, generateSyntheticAccessors = true) {
                                        // TODO: check if this is a correct place to do it
                                        it.visibility = DescriptorVisibilities.PUBLIC
                                        irExpressionBody
                                    }.also {
                                        it.origin = IrDeclarationOrigin.SCRIPT_RESULT_PROPERTY
                                        irScript.statements += it
                                        irScript.resultProperty = it.symbol
                                    }
                            }
                        } else {
                            irScript.statements += irExpressionBody.expression
                        }
                    }
                    is KtDestructuringDeclaration -> {
                        // copied with modifications from StatementGenerator.visitDestructuringDeclaration
                        // TODO: consider code deduplication
                        val bodyGenerator = BodyGenerator(irScript.symbol, context)
                        val statementGenerator = bodyGenerator.createStatementGenerator()
                        val irBlock = IrCompositeImpl(
                            d.startOffsetSkippingComments, d.endOffset,
                            context.irBuiltIns.unitType, IrStatementOrigin.DESTRUCTURING_DECLARATION
                        )
                        val ktInitializer = d.initializer!!
                        val initializerExpr = ktInitializer.deparenthesize().accept(statementGenerator, null).assertCast<IrExpression>()
                        val containerValue =
                            statementGenerator.scope.createTemporaryVariableInBlock(context, initializerExpr, irBlock, "container")

                        val callGenerator = CallGenerator(statementGenerator)

                        for ((index, ktEntry) in d.entries.withIndex()) {
                            val componentResolvedCall = getOrFail(BindingContext.COMPONENT_RESOLVED_CALL, ktEntry)

                            val componentSubstitutedCall = statementGenerator.pregenerateCall(componentResolvedCall)
                            componentSubstitutedCall.setExplicitReceiverValue(containerValue)

                            val componentVariable = getOrFail(BindingContext.VARIABLE, ktEntry)

                            // componentN for '_' SHOULD NOT be evaluated
                            if (componentVariable.name.isSpecial || ktEntry.isSingleUnderscore) continue

                            val irComponentCall = callGenerator.generateCall(
                                ktEntry.startOffsetSkippingComments, ktEntry.endOffset, componentSubstitutedCall,
                                IrStatementOrigin.COMPONENT_N.withIndex(index + 1)
                            )

                            val irComponentProperty =
                                PropertyGenerator(declarationGenerator).generateDestructuringDeclarationEntryAsPropertyDeclaration(
                                    ktEntry, irComponentCall
                                )
                            val irComponentBackingField = irComponentProperty.backingField!!

                            irScript.statements += irComponentProperty

                            val irComponentInitializer = IrSetFieldImpl(
                                ktEntry.startOffsetSkippingComments, ktEntry.endOffset,
                                irComponentBackingField.symbol,
                                context.irBuiltIns.unitType,
                                origin = null, superQualifierSymbol = null
                            ).apply {
                                value = irComponentCall
                                receiver = IrGetValueImpl(
                                    ktEntry.startOffsetSkippingComments, ktEntry.endOffset, irScript.thisReceiver.symbol
                                )
                            }
                            irBlock.statements.add(irComponentInitializer)
                        }
                        irScript.statements += irBlock
                    }
                    else -> irScript.statements += declarationGenerator.generateMemberDeclaration(d)!!
                }
            }
        }
    }

    private fun ParameterDescriptor.toIrValueParameter(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin) =
        context.symbolTable.declareValueParameter(
            startOffset, endOffset, origin,
            this,
            type.toIrType(),
            varargElementType?.toIrType()
        )
}
