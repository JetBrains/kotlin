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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.psi2ir.intermediate.VariableLValue
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsResultOfLambda
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType

class BodyGenerator(
    val scopeOwnerSymbol: IrSymbol,
    override val context: GeneratorContext
) : GeneratorWithScope {

    val scopeOwner: DeclarationDescriptor get() = scopeOwnerSymbol.descriptor

    private val typeTranslator = context.typeTranslator
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    override val scope = Scope(scopeOwnerSymbol)
    private val loopTable = HashMap<KtLoopExpression, IrLoop>()

    fun generateFunctionBody(ktBody: KtExpression): IrBody {
        val statementGenerator = createStatementGenerator()

        val irBlockBody = context.irFactory.createBlockBody(ktBody.startOffsetSkippingComments, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            statementGenerator.generateStatements(ktBody.statements, irBlockBody)
        } else {
            val irBody = statementGenerator.generateStatement(ktBody)
            irBlockBody.statements.add(
                if (ktBody.isUsedAsExpression(context.bindingContext) && irBody is IrExpression)
                    generateReturnExpression(irBody.endOffset, irBody.endOffset, irBody)
                else
                    irBody
            )
        }

        return irBlockBody
    }

    fun generateExpressionBody(ktExpression: KtExpression): IrExpressionBody =
        context.irFactory.createExpressionBody(createStatementGenerator().generateExpression(ktExpression))

    fun generateLambdaBody(ktFun: KtFunctionLiteral): IrBody {
        val statementGenerator = createStatementGenerator()

        val ktBody = ktFun.bodyExpression!!
        val irBlockBody = context.irFactory.createBlockBody(ktBody.startOffsetSkippingComments, ktBody.endOffset)

        for (ktParameter in ktFun.valueParameters) {
            val ktDestructuringDeclaration = ktParameter.destructuringDeclaration ?: continue
            val valueParameter = getOrFail(BindingContext.VALUE_PARAMETER, ktParameter)
            val parameterValue = VariableLValue(
                context,
                ktDestructuringDeclaration.startOffsetSkippingComments, ktDestructuringDeclaration.endOffset,
                context.symbolTable.referenceValue(valueParameter),
                valueParameter.type.toIrType(),
                IrStatementOrigin.DESTRUCTURING_DECLARATION
            )
            statementGenerator.declareComponentVariablesInBlock(ktDestructuringDeclaration, irBlockBody, parameterValue)
        }

        val ktBodyStatements = ktBody.statements
        if (ktBodyStatements.isNotEmpty()) {
            for (ktStatement in ktBodyStatements.dropLast(1)) {
                irBlockBody.statements.add(statementGenerator.generateStatement(ktStatement))
            }
            val ktReturnedValue = ktBodyStatements.last()
            val irReturnedValue = statementGenerator.generateStatement(ktReturnedValue)
            irBlockBody.statements.add(
                if (ktReturnedValue.isUsedAsResultOfLambda(context.bindingContext) && irReturnedValue is IrExpression) {
                    generateReturnExpression(irReturnedValue.startOffset, irReturnedValue.endOffset, irReturnedValue)
                } else {
                    irReturnedValue
                }
            )
        } else {
            irBlockBody.statements.add(
                generateReturnExpression(
                    ktBody.startOffsetSkippingComments, ktBody.endOffset,
                    IrGetObjectValueImpl(
                        ktBody.startOffsetSkippingComments, ktBody.endOffset, context.irBuiltIns.unitType,
                        context.irBuiltIns.unitClass
                    )
                )
            )
        }

        return irBlockBody
    }

    private fun generateReturnExpression(startOffset: Int, endOffset: Int, returnValue: IrExpression): IrReturnImpl {
        val returnTarget = scopeOwnerSymbol.owner as? IrFunction ?: throw AssertionError("'return' in a non-callable: $scopeOwner")
        return IrReturnImpl(
            startOffset, endOffset, context.irBuiltIns.nothingType,
            returnTarget.symbol,
            returnValue
        )
    }

    fun generateSecondaryConstructorBody(ktConstructor: KtSecondaryConstructor): IrBody {
        val irBlockBody = context.irFactory.createBlockBody(ktConstructor.startOffsetSkippingComments, ktConstructor.endOffset)

        generateDelegatingConstructorCall(irBlockBody, ktConstructor)

        ktConstructor.bodyExpression?.let { ktBody ->
            createStatementGenerator().generateStatements(ktBody.statements, irBlockBody)
        }

        return irBlockBody
    }

    private fun generateDelegatingConstructorCall(irBlockBody: IrBlockBody, ktConstructor: KtSecondaryConstructor) {
        val constructorDescriptor = scopeOwner as ClassConstructorDescriptor

        val statementGenerator = createStatementGenerator()
        val ktDelegatingConstructorCall = ktConstructor.getDelegationCall()
        val delegatingConstructorResolvedCall = getResolvedCall(ktDelegatingConstructorCall)

        if (delegatingConstructorResolvedCall == null) {
            val classDescriptor = constructorDescriptor.containingDeclaration
            if (classDescriptor.kind == ClassKind.ENUM_CLASS) {
                generateEnumSuperConstructorCall(irBlockBody, ktConstructor, classDescriptor)
            } else {
                generateAnySuperConstructorCall(irBlockBody, ktConstructor)
            }
            return
        }

        val delegatingConstructorCall = statementGenerator.pregenerateCall(delegatingConstructorResolvedCall)
        val irDelegatingConstructorCall = CallGenerator(statementGenerator).generateDelegatingConstructorCall(
            ktDelegatingConstructorCall.startOffsetSkippingComments, ktDelegatingConstructorCall.endOffset,
            delegatingConstructorCall
        )
        irBlockBody.statements.add(irDelegatingConstructorCall)
    }

    fun createStatementGenerator() = StatementGenerator(this, scope)

    fun putLoop(expression: KtLoopExpression, irLoop: IrLoop) {
        loopTable[expression] = irLoop
    }

    fun getLoop(expression: KtExpression): IrLoop? =
        loopTable[expression]

    fun generatePrimaryConstructorBody(ktClassOrObject: KtPureClassOrObject, irConstructor: IrConstructor): IrBody {
        val irBlockBody = context.irFactory.createBlockBody(ktClassOrObject.pureStartOffset, ktClassOrObject.pureEndOffset)

        generateSuperConstructorCall(irBlockBody, ktClassOrObject)

        val classDescriptor = (scopeOwner as ClassConstructorDescriptor).containingDeclaration
        if (classDescriptor.contextReceivers.isNotEmpty()) {
            generateSetContextReceiverFieldForPrimaryConstructorBody(classDescriptor, irConstructor, irBlockBody)
        }
        irBlockBody.statements.add(
            IrInstanceInitializerCallImpl(
                ktClassOrObject.pureStartOffset, ktClassOrObject.pureEndOffset,
                context.symbolTable.referenceClass(classDescriptor),
                context.irBuiltIns.unitType
            )
        )

        return irBlockBody
    }

    fun generateSecondaryConstructorBodyWithNestedInitializers(ktConstructor: KtSecondaryConstructor): IrBody {
        val irBlockBody = context.irFactory.createBlockBody(ktConstructor.startOffsetSkippingComments, ktConstructor.endOffset)

        generateDelegatingConstructorCall(irBlockBody, ktConstructor)

        val classDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor).containingDeclaration as ClassDescriptor
        irBlockBody.statements.add(
            IrInstanceInitializerCallImpl(
                ktConstructor.startOffsetSkippingComments, ktConstructor.endOffset,
                context.symbolTable.referenceClass(classDescriptor),
                context.irBuiltIns.unitType
            )
        )

        ktConstructor.bodyExpression?.let { ktBody ->
            createStatementGenerator().generateStatements(ktBody.statements, irBlockBody)
        }

        return irBlockBody
    }

    private fun generateSuperConstructorCall(body: IrBlockBody, ktClassOrObject: KtPureClassOrObject) {
        val classDescriptor = ktClassOrObject.findClassDescriptor(context.bindingContext)

        context.extensions.createCustomSuperConstructorCall(ktClassOrObject, classDescriptor, context)?.let {
            body.statements.add(it)
            return
        }

        when (classDescriptor.kind) {
            // enums can't be synthetic
            ClassKind.ENUM_CLASS -> generateEnumSuperConstructorCall(body, ktClassOrObject as KtClassOrObject, classDescriptor)

            ClassKind.ENUM_ENTRY -> {
                body.statements.add(
                    generateEnumEntrySuperConstructorCall(ktClassOrObject as KtEnumEntry, classDescriptor)
                )
            }

            else -> {
                val statementGenerator = createStatementGenerator()

                // synthetic inheritance is not supported yet
                (ktClassOrObject as? KtClassOrObject)?.getSuperTypeList()?.let { ktSuperTypeList ->
                    for (ktSuperTypeListEntry in ktSuperTypeList.entries) {
                        if (ktSuperTypeListEntry is KtSuperTypeCallEntry) {
                            val resolvedCall = getResolvedCall(ktSuperTypeListEntry) ?: continue
                            val superConstructorCall = statementGenerator.pregenerateCall(resolvedCall)
                            val irSuperConstructorCall = CallGenerator(statementGenerator).generateDelegatingConstructorCall(
                                ktSuperTypeListEntry.startOffsetSkippingComments, ktSuperTypeListEntry.endOffset, superConstructorCall
                            )
                            body.statements.add(irSuperConstructorCall)
                            return
                        }
                    }
                }

                // If we are here, we didn't find a superclass entry in super types.
                // Thus, super class should be Any.
                val superClass = classDescriptor.getSuperClassOrAny()
                assert(KotlinBuiltIns.isAny(superClass)) {
                    "$classDescriptor: Super class should be any: $superClass"
                }
                generateAnySuperConstructorCall(body, ktClassOrObject)
            }
        }
    }

    private fun generateAnySuperConstructorCall(body: IrBlockBody, ktElement: KtPureElement) {
        val anyConstructor = context.irBuiltIns.anyClass.descriptor.constructors.single()
        body.statements.add(
            IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                ktElement.pureStartOffset, ktElement.pureEndOffset,
                context.irBuiltIns.unitType,
                context.symbolTable.referenceConstructor(anyConstructor)
            )
        )
    }

    private fun generateEnumSuperConstructorCall(body: IrBlockBody, ktElement: KtElement, classDescriptor: ClassDescriptor) {
        val enumConstructor = context.irBuiltIns.enumClass.descriptor.constructors.single()
        body.statements.add(
            IrEnumConstructorCallImpl.fromSymbolDescriptor(
                ktElement.startOffsetSkippingComments, ktElement.endOffset,
                context.irBuiltIns.unitType,
                context.symbolTable.referenceConstructor(enumConstructor),
                1 // kotlin.Enum<T> has a single type parameter
            ).apply {
                putTypeArgument(0, classDescriptor.defaultType.toIrType())
            }
        )
    }

    private fun generateEnumEntrySuperConstructorCall(ktEnumEntry: KtEnumEntry, enumEntryDescriptor: ClassDescriptor): IrExpression {
        return generateEnumConstructorCallOrSuperCall(ktEnumEntry, enumEntryDescriptor.containingDeclaration as ClassDescriptor)
    }

    fun generateEnumEntryInitializer(ktEnumEntry: KtEnumEntry, enumEntryDescriptor: ClassDescriptor): IrExpression {
        if (ktEnumEntry.declarations.isNotEmpty()) {
            val enumEntryConstructor = enumEntryDescriptor.unsubstitutedPrimaryConstructor!!
            return IrEnumConstructorCallImpl.fromSymbolDescriptor(
                ktEnumEntry.startOffsetSkippingComments, ktEnumEntry.endOffset,
                context.irBuiltIns.unitType,
                context.symbolTable.referenceConstructor(enumEntryConstructor),
                0 // enums can't be generic
            )
        }

        return generateEnumConstructorCallOrSuperCall(ktEnumEntry, enumEntryDescriptor.containingDeclaration as ClassDescriptor)
    }

    private fun generateEnumConstructorCallOrSuperCall(
        ktEnumEntry: KtEnumEntry,
        enumClassDescriptor: ClassDescriptor
    ): IrExpression {
        val statementGenerator = createStatementGenerator()

        // Entry constructor with argument(s)
        val ktSuperCallElement = ktEnumEntry.superTypeListEntries.firstOrNull()
        if (ktSuperCallElement != null) {
            return statementGenerator.generateEnumConstructorCall(getResolvedCall(ktSuperCallElement)!!, ktEnumEntry)
        }

        val enumDefaultConstructorCall = getResolvedCall(ktEnumEntry)
            ?: throw AssertionError("No default constructor call for enum entry $enumClassDescriptor")
        return statementGenerator.generateEnumConstructorCall(enumDefaultConstructorCall, ktEnumEntry)
    }

    private fun StatementGenerator.generateEnumConstructorCall(
        constructorCall: ResolvedCall<out CallableDescriptor>,
        ktEnumEntry: KtEnumEntry
    ) =
        CallGenerator(this).generateEnumConstructorSuperCall(
            ktEnumEntry.startOffsetSkippingComments, ktEnumEntry.endOffset,
            pregenerateCall(constructorCall)
        )

    private fun generateSetContextReceiverFieldForPrimaryConstructorBody(
        classDescriptor: ClassDescriptor,
        irConstructor: IrConstructor,
        irBlockBody: IrBlockBody
    ) {
        val thisAsReceiverParameter = classDescriptor.thisAsReceiverParameter
        val receiver = IrGetValueImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            thisAsReceiverParameter.type.toIrType(),
            context.symbolTable.referenceValue(thisAsReceiverParameter)
        )
        for ((index, receiverDescriptor) in classDescriptor.contextReceivers.withIndex()) {
            val irValueParameter = irConstructor.valueParameters[index]
            irBlockBody.statements.add(
                IrSetFieldImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.additionalDescriptorStorage.getSyntheticField(receiverDescriptor.value).symbol,
                    receiver,
                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irValueParameter.type, irValueParameter.symbol),
                    context.irBuiltIns.unitType
                )
            )
        }
    }
}
