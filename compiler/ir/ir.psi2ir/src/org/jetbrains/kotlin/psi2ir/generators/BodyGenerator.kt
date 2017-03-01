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
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionBase
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.VariableLValue
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import java.lang.AssertionError
import java.util.*

class BodyGenerator(val scopeOwner: DeclarationDescriptor, override val context: GeneratorContext) : GeneratorWithScope {
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
            irFunction.putDefault(valueParameter, IrExpressionBodyImpl(irDefaultValue))
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
            IrExpressionBodyImpl(createStatementGenerator().generateExpression(ktInitializer))

    fun generateLambdaBody(ktFun: KtFunctionLiteral): IrBody {
        val statementGenerator = createStatementGenerator()

        val ktBody = ktFun.bodyExpression!!
        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)

        for (ktParameter in ktFun.valueParameters) {
            val ktDestructuringDeclaration = ktParameter.destructuringDeclaration ?: continue
            val valueParameter = getOrFail(BindingContext.VALUE_PARAMETER, ktParameter)
            val parameterValue = VariableLValue(ktDestructuringDeclaration.startOffset, ktDestructuringDeclaration.endOffset,
                                                valueParameter, IrStatementOrigin.DESTRUCTURING_DECLARATION)
            statementGenerator.declareComponentVariablesInBlock(ktDestructuringDeclaration, irBlockBody, parameterValue)
        }

        if (ktBody is KtBlockExpression) {
            val ktBodyStatements = ktBody.statements
            if (ktBodyStatements.isNotEmpty()) {
                for (ktStatement in ktBodyStatements.dropLast(1)) {
                    irBlockBody.statements.add(statementGenerator.generateStatement(ktStatement))
                }
                val ktReturnedValue = ktBodyStatements.last()
                statementGenerator.generateReturnExpression(ktReturnedValue, irBlockBody)
            }
            else {
                irBlockBody.statements.add(generateReturnExpression(
                        ktBody.startOffset, ktBody.endOffset,
                        IrGetObjectValueImpl(ktBody.startOffset, ktBody.endOffset, context.builtIns.unitType, context.builtIns.unit)))
            }
        }

        return irBlockBody
    }

    private fun StatementGenerator.generateBlockBodyStatements(irBlockBody: IrBlockBodyImpl, ktBody: KtBlockExpression) {
        for (ktStatement in ktBody.statements) {
            irBlockBody.statements.add(generateStatement(ktStatement))
        }
    }

    private fun StatementGenerator.generateReturnExpression(ktExpression: KtExpression, irBlockBody: IrBlockBodyImpl) {
        val irReturnExpression = generateStatement(ktExpression)
        if (irReturnExpression is IrExpression) {
            irBlockBody.statements.add(irReturnExpression.wrapWithReturn())
        }
        else {
            irBlockBody.statements.add(irReturnExpression)
        }
    }

    private fun IrExpression.wrapWithReturn() =
            if (this is IrReturn || this is IrErrorExpression || this is IrThrow)
                this
            else {
                generateReturnExpression(startOffset, endOffset, this) }


    private fun generateReturnExpression(startOffset: Int, endOffset: Int, returnValue: IrExpression): IrReturnImpl {
        val returnTarget = (scopeOwner as? CallableDescriptor) ?:
                           throw AssertionError("'return' in a non-callable: $scopeOwner")
        return IrReturnImpl(startOffset, endOffset, context.builtIns.nothingType, returnTarget, returnValue)
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
        val constructorDescriptor = scopeOwner as ClassConstructorDescriptor

        val statementGenerator = createStatementGenerator()
        val ktDelegatingConstructorCall = ktConstructor.getDelegationCall()
        val delegatingConstructorResolvedCall = getResolvedCall(ktDelegatingConstructorCall)

        if (delegatingConstructorResolvedCall == null) {
            if (constructorDescriptor.containingDeclaration.kind == ClassKind.ENUM_CLASS) {
                generateEnumSuperConstructorCall(irBlockBody, ktConstructor)
            }
            else {
                generateAnySuperConstructorCAll(irBlockBody, ktConstructor)
            }
            return
        }

        val delegatingConstructorCall = statementGenerator.pregenerateCall(delegatingConstructorResolvedCall)
        val irDelegatingConstructorCall = CallGenerator(statementGenerator).generateDelegatingConstructorCall(
                ktDelegatingConstructorCall.startOffset, ktDelegatingConstructorCall.endOffset,
                delegatingConstructorCall)
        irBlockBody.statements.add(irDelegatingConstructorCall)
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

        val classDescriptor = (scopeOwner as ClassConstructorDescriptor).containingDeclaration
        irBlockBody.statements.add(IrInstanceInitializerCallImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset, classDescriptor))

        return irBlockBody
    }

    fun generateSecondaryConstructorBodyWithNestedInitializers(ktConstructor: KtSecondaryConstructor): IrBody {
        val irBlockBody = IrBlockBodyImpl(ktConstructor.startOffset, ktConstructor.endOffset)

        generateDelegatingConstructorCall(irBlockBody, ktConstructor)

        val classDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor).containingDeclaration as ClassDescriptor
        irBlockBody.statements.add(IrInstanceInitializerCallImpl(ktConstructor.startOffset, ktConstructor.endOffset, classDescriptor))

        ktConstructor.bodyExpression?.let { ktBody ->
            createStatementGenerator().generateBlockBodyStatements(irBlockBody, ktBody)
        }

        return irBlockBody
    }

    private fun generateSuperConstructorCall(irBlockBody: IrBlockBodyImpl, ktClassOrObject: KtClassOrObject) {
        val classDescriptor = getOrFail(BindingContext.CLASS, ktClassOrObject)

        when (classDescriptor.kind) {
            ClassKind.ENUM_CLASS -> {
                generateEnumSuperConstructorCall(irBlockBody, ktClassOrObject)
            }
            ClassKind.ENUM_ENTRY -> {
                irBlockBody.statements.add(generateEnumEntrySuperConstructorCall(ktClassOrObject as KtEnumEntry, classDescriptor))
            }
            else -> {
                val statementGenerator = createStatementGenerator()

                ktClassOrObject.getSuperTypeList()?.let { ktSuperTypeList ->
                    for (ktSuperTypeListEntry in ktSuperTypeList.entries) {
                        if (ktSuperTypeListEntry is KtSuperTypeCallEntry) {
                            val superConstructorCall = statementGenerator.pregenerateCall(getResolvedCall(ktSuperTypeListEntry)!!)
                            val irSuperConstructorCall = CallGenerator(statementGenerator).generateDelegatingConstructorCall(
                                    ktSuperTypeListEntry.startOffset, ktSuperTypeListEntry.endOffset, superConstructorCall)
                            irBlockBody.statements.add(irSuperConstructorCall)
                            return
                        }
                    }
                }

                // If we are here, we didn't find a superclass entry in super types.
                // Thus, super class should be Any.
                val superClass = classDescriptor.getSuperClassOrAny()
                assert(KotlinBuiltIns.isAny(superClass)) { "$classDescriptor: Super class should be any: $superClass" }
                generateAnySuperConstructorCAll(irBlockBody, ktClassOrObject)
            }
        }
    }

    private fun generateAnySuperConstructorCAll(irBlockBody: IrBlockBodyImpl, ktElement: KtElement) {
        val anyConstructor = context.builtIns.any.constructors.single()
        irBlockBody.statements.add(IrDelegatingConstructorCallImpl(ktElement.startOffset, ktElement.endOffset, anyConstructor, null))
    }

    private fun generateEnumSuperConstructorCall(irBlockBody: IrBlockBodyImpl, ktElement: KtElement) {
        val enumConstructor = context.builtIns.enum.constructors.single()
        irBlockBody.statements.add(IrEnumConstructorCallImpl(ktElement.startOffset, ktElement.endOffset, enumConstructor))
    }

    private fun generateEnumEntrySuperConstructorCall(ktEnumEntry: KtEnumEntry, enumEntryDescriptor: ClassDescriptor): IrExpression {
        return generateEnumConstructorCallOrSuperCall(ktEnumEntry, enumEntryDescriptor.containingDeclaration as ClassDescriptor)
    }

    fun generateAnonymousInitializerBody(ktAnonymousInitializer: KtAnonymousInitializer): IrBlockBody {
        val ktBody = ktAnonymousInitializer.body!!
        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            createStatementGenerator().generateBlockBodyStatements(irBlockBody, ktBody)
        }
        else {
            irBlockBody.statements.add(createStatementGenerator().generateStatement(ktBody))
        }
        return irBlockBody
    }

    fun generateEnumEntryInitializer(ktEnumEntry: KtEnumEntry, enumEntryDescriptor: ClassDescriptor): IrExpression {
        if (ktEnumEntry.declarations.isNotEmpty()) {
            val enumEntryConstructor = enumEntryDescriptor.unsubstitutedPrimaryConstructor!!
            return IrEnumConstructorCallImpl(ktEnumEntry.startOffset, ktEnumEntry.endOffset, enumEntryConstructor)
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
        if (enumDefaultConstructorCall != null) {
            return statementGenerator.generateEnumConstructorCall(enumDefaultConstructorCall, ktEnumEntry)
        }

        // No-argument enum entry constructor
        val enumClassConstructor = enumClassDescriptor.constructors.find { it.valueParameters.isEmpty() }!!
        return IrEnumConstructorCallImpl(ktEnumEntry.startOffset, ktEnumEntry.endOffset, enumClassConstructor)
    }

    private fun StatementGenerator.generateEnumConstructorCall(constructorCall: ResolvedCall<out CallableDescriptor>, ktEnumEntry: KtEnumEntry) =
            CallGenerator(this).generateEnumConstructorSuperCall(ktEnumEntry.startOffset, ktEnumEntry.endOffset,
                                                                 pregenerateCall(constructorCall))

}

