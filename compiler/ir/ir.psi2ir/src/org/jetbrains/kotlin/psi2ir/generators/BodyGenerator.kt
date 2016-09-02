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
import org.jetbrains.kotlin.ir.declarations.IrFunctionBase
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.builders.irBlockBody
import org.jetbrains.kotlin.psi2ir.builders.irCallableReference
import org.jetbrains.kotlin.psi2ir.builders.irGet
import org.jetbrains.kotlin.psi2ir.builders.irReturn
import org.jetbrains.kotlin.psi2ir.intermediate.VariableLValue
import org.jetbrains.kotlin.psi2ir.intermediate.setExplicitReceiverValue
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.*

class BodyGenerator(val scopeOwner: DeclarationDescriptor, override val context: GeneratorContext): GeneratorWithScope {
    override val scope = Scope(scopeOwner)
    private val loopTable = HashMap<KtLoopExpression, IrLoop>()

    fun generateDefaultParameters(ktFunction: KtFunction, irFunction: IrFunctionBase) {
        generateDefaultParameters(ktFunction.valueParameterList ?: return, irFunction)
    }

    fun generateDefaultParameters(ktParameterList: KtParameterList, irFunction: IrFunctionBase) {
        val statementGenerator = createStatementGenerator()
        for (ktParameter in ktParameterList.parameters) {
            val ktDefaultValue = ktParameter.defaultValue ?: continue
            val valueParameter = getOrFail(BindingContext.VALUE_PARAMETER, ktParameter) as? ValueParameterDescriptor ?: continue
            val irDefaultValue = statementGenerator.generateExpression(ktDefaultValue)
            irFunction.putDefault(valueParameter, IrExpressionBodyImpl(ktDefaultValue.startOffset, ktDefaultValue.endOffset, irDefaultValue))
        }
    }

    fun generateFunctionBody(ktBody: KtExpression): IrBody {
        val statementGenerator = createStatementGenerator()

        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            statementGenerator.generateBlockBodyStatements(irBlockBody, ktBody)
        }
        else {
            statementGenerator.generateReturnExpression(ktBody, irBlockBody)
        }

        return irBlockBody
    }

    fun generatePropertyInitializerBody(ktInitializer: KtExpression): IrExpressionBody =
            IrExpressionBodyImpl(ktInitializer.startOffset, ktInitializer.endOffset,
                                 createStatementGenerator().generateExpression(ktInitializer))

    fun generateLambdaBody(ktFun: KtFunctionLiteral): IrBody {
        val statementGenerator = createStatementGenerator()

        val ktBody = ktFun.bodyExpression!!
        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            for (ktStatement in ktBody.statements.subList(0, ktBody.statements.size - 1)) {
                irBlockBody.addStatement(statementGenerator.generateStatement(ktStatement))
            }
            val ktReturnedValue = ktBody.statements.last()
            statementGenerator.generateReturnExpression(ktReturnedValue, irBlockBody)
        }

        return irBlockBody
    }

    private fun StatementGenerator.generateBlockBodyStatements(irBlockBody: IrBlockBodyImpl, ktBody: KtBlockExpression) {
        for (ktStatement in ktBody.statements) {
            irBlockBody.addStatement(generateStatement(ktStatement))
        }
    }

    private fun StatementGenerator.generateReturnExpression(ktExpression: KtExpression, irBlockBody: IrBlockBodyImpl) {
        val irExpression = generateExpression(ktExpression)
        irBlockBody.addStatement(irExpression.wrapWithReturn())
    }

    private fun IrExpression.wrapWithReturn() =
            if (KotlinBuiltIns.isNothing(type))
                this
            else {
                val returnTarget = (scopeOwner as? CallableDescriptor) ?:
                                   throw AssertionError("'return' in a non-callable: $scopeOwner")
                IrReturnImpl(startOffset, endOffset, context.builtIns.nothingType,
                             returnTarget, this)
            }


    fun generateSecondaryConstructorBody(ktConstructor: KtSecondaryConstructor): IrBody {
        val irBlockBody = IrBlockBodyImpl(ktConstructor.startOffset, ktConstructor.endOffset)

        generateDelegatingConstructorCall(irBlockBody, ktConstructor)

        ktConstructor.bodyExpression?.let { ktBody ->
            createStatementGenerator().generateBlockBodyStatements(irBlockBody, ktBody)
        }

        return irBlockBody
    }

    private fun generateDelegatingConstructorCall(irBlockBody: IrBlockBodyImpl, ktConstructor: KtSecondaryConstructor) {
        val statementGenerator = createStatementGenerator()
        val ktDelegatingConstructorCall = ktConstructor.getDelegationCall()
        val delegatingConstructorCall = statementGenerator.pregenerateCall(getResolvedCall(ktDelegatingConstructorCall)!!)
        val irDelegatingConstructorCall = CallGenerator(statementGenerator).generateDelegatingConstructorCall(
                ktDelegatingConstructorCall.startOffset, ktDelegatingConstructorCall.endOffset,
                delegatingConstructorCall)
        irBlockBody.addStatement(irDelegatingConstructorCall)
    }

    fun createStatementGenerator() = StatementGenerator(this, scope)

    fun putLoop(expression: KtLoopExpression, irLoop: IrLoop) {
        loopTable[expression] = irLoop
    }

    fun getLoop(expression: KtExpression): IrLoop? =
            loopTable[expression]

    fun generatePrimaryConstructorBody(ktClassOrObject: KtClassOrObject): IrBody {
        val irBlockBody = IrBlockBodyImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset)

        generateSuperConstructorCall(irBlockBody, ktClassOrObject)
        generateInitializersForPropertiesDefinedInPrimaryConstructor(irBlockBody, ktClassOrObject)

        val classDescriptor = (scopeOwner as ConstructorDescriptor).containingDeclaration
        irBlockBody.addStatement(IrInstanceInitializerCallImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset, classDescriptor))

        return irBlockBody
    }

    fun generateSecondaryConstructorBodyWithNestedInitializers(ktConstructor: KtSecondaryConstructor): IrBody {
        val irBlockBody = IrBlockBodyImpl(ktConstructor.startOffset, ktConstructor.endOffset)

        generateDelegatingConstructorCall(irBlockBody, ktConstructor)

        val classDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor).containingDeclaration
        irBlockBody.addStatement(IrInstanceInitializerCallImpl(ktConstructor.startOffset, ktConstructor.endOffset, classDescriptor))

        ktConstructor.bodyExpression?.let { ktBody ->
            createStatementGenerator().generateBlockBodyStatements(irBlockBody, ktBody)
        }

        return irBlockBody
    }

    private fun generateSuperConstructorCall(irBlockBody: IrBlockBodyImpl, ktClassOrObject: KtClassOrObject) {
        val classDescriptor = getOrFail(BindingContext.CLASS, ktClassOrObject)

        when (classDescriptor.kind) {
            ClassKind.ENUM_CLASS -> {
                val kotlinEnumConstructor = context.builtIns.enum.unsubstitutedPrimaryConstructor!!
                irBlockBody.addStatement(IrEnumConstructorCallImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset,
                                                                   kotlinEnumConstructor, null))
            }
            ClassKind.ENUM_ENTRY -> {
                irBlockBody.addStatement(generateEnumEntrySuperConstructorCall(ktClassOrObject as KtEnumEntry, classDescriptor))
            }
            else -> {
                val ktSuperTypeList = ktClassOrObject.getSuperTypeList() ?: return
                for (ktSuperTypeListEntry in ktSuperTypeList.entries) {
                    if (ktSuperTypeListEntry is KtSuperTypeCallEntry) {
                        val statementGenerator = createStatementGenerator()
                        val superConstructorCall = statementGenerator.pregenerateCall(getResolvedCall(ktSuperTypeListEntry)!!)
                        val irSuperConstructorCall = CallGenerator(statementGenerator).generateCall(
                                ktSuperTypeListEntry, superConstructorCall, IrOperator.SUPER_CONSTRUCTOR_CALL)
                        irBlockBody.addStatement(irSuperConstructorCall)
                    }
                }
            }
        }
    }

    private fun generateEnumEntrySuperConstructorCall(ktEnumEntry: KtEnumEntry, enumEntryDescriptor: ClassDescriptor): IrExpression {
        return generateEnumConstructorCallOrSuperCall(ktEnumEntry, enumEntryDescriptor.containingDeclaration as ClassDescriptor, null)
    }

    fun generateAnonymousInitializerBody(ktAnonymousInitializer: KtAnonymousInitializer): IrBlockBody {
        val ktBody = ktAnonymousInitializer.body!!
        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            createStatementGenerator().generateBlockBodyStatements(irBlockBody, ktBody)
        }
        else {
            irBlockBody.addStatement(createStatementGenerator().generateStatement(ktBody))
        }
        return irBlockBody
    }

    fun generateEnumEntryInitializer(ktEnumEntry: KtEnumEntry, enumEntryDescriptor: ClassDescriptor): IrExpression {
        if (ktEnumEntry.declarations.isNotEmpty()) {
            val enumEntryConstructor = enumEntryDescriptor.unsubstitutedPrimaryConstructor!!
            return IrEnumConstructorCallImpl(ktEnumEntry.startOffset, ktEnumEntry.endOffset,
                                             enumEntryConstructor, enumEntryDescriptor)
        }

        return generateEnumConstructorCallOrSuperCall(ktEnumEntry, enumEntryDescriptor.containingDeclaration as ClassDescriptor, enumEntryDescriptor)
    }

    private fun generateEnumConstructorCallOrSuperCall(
            ktEnumEntry: KtEnumEntry,
            enumClassDescriptor: ClassDescriptor,
            enumEntryOrNull: ClassDescriptor?
    ): IrExpression {
        val statementGenerator = createStatementGenerator()

        // Entry constructor with argument(s)
        ktEnumEntry.getSuperTypeListEntries().firstOrNull()?.let { ktSuperCallElement ->
            val enumConstructorCall = statementGenerator.pregenerateCall(getResolvedCall(ktSuperCallElement)!!)
            return CallGenerator(statementGenerator).generateEnumConstructorSuperCall(
                    ktEnumEntry.startOffset, ktEnumEntry.endOffset,
                    enumConstructorCall, enumEntryOrNull)

        }

        // No-argument enum entry constructor
        val enumClassConstructor = enumClassDescriptor.unsubstitutedPrimaryConstructor!!
        return IrEnumConstructorCallImpl(ktEnumEntry.startOffset, ktEnumEntry.endOffset,
                                         enumClassConstructor, enumEntryOrNull)
    }

    private fun generateInitializersForPropertiesDefinedInPrimaryConstructor(irBlockBody: IrBlockBodyImpl, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getPrimaryConstructor()?.let { ktPrimaryConstructor ->
            for (ktParameter in ktPrimaryConstructor.valueParameters) {
                if (ktParameter.hasValOrVar()) {
                    val propertyDescriptor = getOrFail(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, ktParameter)
                    val valueParameterDescriptor = getOrFail(BindingContext.VALUE_PARAMETER, ktParameter)

                    irBlockBody.addStatement(
                            createPropertyInitializationExpression(
                                    ktParameter, propertyDescriptor,
                                    IrGetVariableImpl(ktParameter.startOffset, ktParameter.endOffset,
                                                      valueParameterDescriptor, IrOperator.INITIALIZE_PROPERTY_FROM_PARAMETER)
                            ))
                }
            }
        }
    }

    private fun createPropertyInitializationExpression(ktElement: KtElement, propertyDescriptor: PropertyDescriptor, value: IrExpression) =
            IrSetBackingFieldImpl(ktElement.startOffset, ktElement.endOffset, propertyDescriptor, value)
}

