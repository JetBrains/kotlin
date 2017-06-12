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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.VariableLValue
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import java.lang.AssertionError
import java.util.*

class BodyGenerator(
        val scopeOwnerSymbol: IrSymbol,
        override val context: GeneratorContext
) : GeneratorWithScope {
    val scopeOwner: DeclarationDescriptor get() = scopeOwnerSymbol.descriptor

    override val scope = Scope(scopeOwnerSymbol)
    private val loopTable = HashMap<KtLoopExpression, IrLoop>()

    fun generateFunctionBody(ktBody: KtExpression): IrBody {
        val statementGenerator = createStatementGenerator()

        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            statementGenerator.generateStatements(ktBody.statements, irBlockBody)
        }
        else {
            statementGenerator.generateReturnExpression(ktBody, irBlockBody)
        }

        return irBlockBody
    }

    fun generateExpressionBody(ktExpression: KtExpression): IrExpressionBody =
            IrExpressionBodyImpl(createStatementGenerator().generateExpression(ktExpression))

    fun generateLambdaBody(ktFun: KtFunctionLiteral): IrBody {
        val statementGenerator = createStatementGenerator()

        val ktBody = ktFun.bodyExpression!!
        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)

        for (ktParameter in ktFun.valueParameters) {
            val ktDestructuringDeclaration = ktParameter.destructuringDeclaration ?: continue
            val valueParameter = getOrFail(BindingContext.VALUE_PARAMETER, ktParameter)
            val parameterValue = VariableLValue(ktDestructuringDeclaration.startOffset, ktDestructuringDeclaration.endOffset,
                                                context.symbolTable.referenceValue(valueParameter),
                                                IrStatementOrigin.DESTRUCTURING_DECLARATION)
            statementGenerator.declareComponentVariablesInBlock(ktDestructuringDeclaration, irBlockBody, parameterValue)
        }

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
                    IrGetObjectValueImpl(ktBody.startOffset, ktBody.endOffset, context.builtIns.unitType,
                                         context.symbolTable.referenceClass(context.builtIns.unit))))
        }

        return irBlockBody
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
        return IrReturnImpl(startOffset, endOffset, context.builtIns.nothingType,
                            context.symbolTable.referenceFunction(returnTarget),
                            returnValue)
    }


    fun generateSecondaryConstructorBody(ktConstructor: KtSecondaryConstructor): IrBody {
        val irBlockBody = IrBlockBodyImpl(ktConstructor.startOffset, ktConstructor.endOffset)

        generateDelegatingConstructorCall(irBlockBody, ktConstructor)

        ktConstructor.bodyExpression?.let { ktBody ->
            createStatementGenerator().generateStatements(ktBody.statements, irBlockBody)
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
                generateAnySuperConstructorCall(irBlockBody, ktConstructor)
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
        irBlockBody.statements.add(IrInstanceInitializerCallImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset,
                                                                 context.symbolTable.referenceClass(classDescriptor)))

        return irBlockBody
    }

    fun generateSecondaryConstructorBodyWithNestedInitializers(ktConstructor: KtSecondaryConstructor): IrBody {
        val irBlockBody = IrBlockBodyImpl(ktConstructor.startOffset, ktConstructor.endOffset)

        generateDelegatingConstructorCall(irBlockBody, ktConstructor)

        val classDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor).containingDeclaration as ClassDescriptor
        irBlockBody.statements.add(IrInstanceInitializerCallImpl(ktConstructor.startOffset, ktConstructor.endOffset,
                                                                 context.symbolTable.referenceClass(classDescriptor)))

        ktConstructor.bodyExpression?.let { ktBody ->
            createStatementGenerator().generateStatements(ktBody.statements, irBlockBody)
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
                generateAnySuperConstructorCall(irBlockBody, ktClassOrObject)
            }
        }
    }

    private fun generateAnySuperConstructorCall(irBlockBody: IrBlockBodyImpl, ktElement: KtElement) {
        val anyConstructor = context.builtIns.any.constructors.single()
        irBlockBody.statements.add(
                IrDelegatingConstructorCallImpl(
                        ktElement.startOffset, ktElement.endOffset,
                        context.symbolTable.referenceConstructor(anyConstructor),
                        anyConstructor,
                        null
                )
        )
    }

    private fun generateEnumSuperConstructorCall(irBlockBody: IrBlockBodyImpl, ktElement: KtElement) {
        val enumConstructor = context.builtIns.enum.constructors.single()
        irBlockBody.statements.add(
                IrEnumConstructorCallImpl(
                        ktElement.startOffset, ktElement.endOffset,
                        context.symbolTable.referenceConstructor(enumConstructor)
                )
        )
    }

    private fun generateEnumEntrySuperConstructorCall(ktEnumEntry: KtEnumEntry, enumEntryDescriptor: ClassDescriptor): IrExpression {
        return generateEnumConstructorCallOrSuperCall(ktEnumEntry, enumEntryDescriptor.containingDeclaration as ClassDescriptor)
    }

    fun generateEnumEntryInitializer(ktEnumEntry: KtEnumEntry, enumEntryDescriptor: ClassDescriptor): IrExpression {
        if (ktEnumEntry.declarations.isNotEmpty()) {
            val enumEntryConstructor = enumEntryDescriptor.unsubstitutedPrimaryConstructor!!
            return IrEnumConstructorCallImpl(
                    ktEnumEntry.startOffset, ktEnumEntry.endOffset,
                    context.symbolTable.referenceConstructor(enumEntryConstructor)
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
        if (enumDefaultConstructorCall != null) {
            return statementGenerator.generateEnumConstructorCall(enumDefaultConstructorCall, ktEnumEntry)
        }

        // Default enum entry constructor
        val enumClassConstructor =
                enumClassDescriptor.constructors.singleOrNull { it.valueParameters.all { it.hasDefaultValue() } } ?:
                throw AssertionError("Enum class $enumClassDescriptor should have a default constructor")
        return IrEnumConstructorCallImpl(
                ktEnumEntry.startOffset, ktEnumEntry.endOffset,
                context.symbolTable.referenceConstructor(enumClassConstructor)
        )
    }

    private fun StatementGenerator.generateEnumConstructorCall(constructorCall: ResolvedCall<out CallableDescriptor>, ktEnumEntry: KtEnumEntry) =
            CallGenerator(this).generateEnumConstructorSuperCall(ktEnumEntry.startOffset, ktEnumEntry.endOffset,
                                                                 pregenerateCall(constructorCall))

}

