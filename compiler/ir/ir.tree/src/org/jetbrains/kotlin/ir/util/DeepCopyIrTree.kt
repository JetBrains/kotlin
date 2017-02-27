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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

open class DeepCopyIrTree : IrElementTransformerVoid() {
    protected open fun mapDeclarationOrigin(declarationOrigin: IrDeclarationOrigin) = declarationOrigin
    protected open fun mapStatementOrigin(statementOrigin: IrStatementOrigin?) = statementOrigin
    protected open fun mapFileEntry(fileEntry: SourceManager.FileEntry) = fileEntry

    protected open fun mapModuleDescriptor(descriptor: ModuleDescriptor) = descriptor
    protected open fun mapPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor) = descriptor
    protected open fun mapClassDeclaration(descriptor: ClassDescriptor) = descriptor
    protected open fun mapTypeAliasDeclaration(descriptor: TypeAliasDescriptor) = descriptor
    protected open fun mapFunctionDeclaration(descriptor: FunctionDescriptor) = descriptor
    protected open fun mapConstructorDeclaration(descriptor: ClassConstructorDescriptor) = descriptor
    protected open fun mapPropertyDeclaration(descriptor: PropertyDescriptor) = descriptor
    protected open fun mapLocalPropertyDeclaration(descriptor: VariableDescriptorWithAccessors) = descriptor
    protected open fun mapEnumEntryDeclaration(descriptor: ClassDescriptor) = descriptor
    protected open fun mapVariableDeclaration(descriptor: VariableDescriptor) = descriptor
    protected open fun mapCatchParameterDeclaration(descriptor: VariableDescriptor) = mapVariableDeclaration(descriptor)
    protected open fun mapErrorDeclaration(descriptor: DeclarationDescriptor) = descriptor

    protected open fun mapSuperQualifier(qualifier: ClassDescriptor?) = qualifier
    protected open fun mapClassReference(descriptor: ClassDescriptor) = descriptor
    protected open fun mapValueReference(descriptor: ValueDescriptor) = descriptor
    protected open fun mapVariableReference(descriptor: VariableDescriptor) = descriptor
    protected open fun mapPropertyReference(descriptor: PropertyDescriptor) = descriptor
    protected open fun mapCallee(descriptor: CallableDescriptor) = descriptor
    protected open fun mapDelegatedConstructorCallee(descriptor: ClassConstructorDescriptor) = descriptor
    protected open fun mapEnumConstructorCallee(descriptor: ClassConstructorDescriptor) = descriptor
    protected open fun mapCallableReference(descriptor: CallableDescriptor) = descriptor
    protected open fun mapClassifierReference(descriptor: ClassifierDescriptor) = descriptor
    protected open fun mapReturnTarget(descriptor: CallableDescriptor) = mapCallee(descriptor)

    override fun visitElement(element: IrElement): IrElement =
            throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment =
            IrModuleFragmentImpl(
                    mapModuleDescriptor(declaration.descriptor),
                    declaration.irBuiltins,
                    declaration.files.map { it.transform(this, null) }
            )

    override fun visitFile(declaration: IrFile): IrFile =
            IrFileImpl(
                    mapFileEntry(declaration.fileEntry),
                    mapPackageFragmentDescriptor(declaration.packageFragmentDescriptor),
                    declaration.fileAnnotations.toMutableList(),
                    declaration.declarations.map { it.transform(this, null) as IrDeclaration }
            )

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement =
            throw IllegalArgumentException("Unsupported declaration type: $declaration")

    override fun visitClass(declaration: IrClass): IrClass =
            IrClassImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapClassDeclaration(declaration.descriptor),
                    declaration.declarations.map { it.transform(this, null) as IrDeclaration }
            )

    override fun visitTypeAlias(declaration: IrTypeAlias): IrTypeAlias =
            IrTypeAliasImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapTypeAliasDeclaration(declaration.descriptor)
            )

    override fun visitFunction(declaration: IrFunction): IrFunction =
            IrFunctionImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapFunctionDeclaration(declaration.descriptor),
                    declaration.body?.transform(this, null)
            ).transformDefaults(declaration)

    override fun visitConstructor(declaration: IrConstructor): IrConstructor =
            IrConstructorImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapConstructorDeclaration(declaration.descriptor),
                    declaration.body!!.transform(this, null)
            ).transformDefaults(declaration)

    private fun <T : IrFunction> T.transformDefaults(original: T): T {
        for (originalValueParameter in original.descriptor.valueParameters) {
            val valueParameter = descriptor.valueParameters[originalValueParameter.index]
            original.getDefault(originalValueParameter)?.let { irDefaultParameterValue ->
                putDefault(valueParameter, irDefaultParameterValue.transform(this@DeepCopyIrTree, null))
            }
        }
        return this
    }

    override fun visitProperty(declaration: IrProperty): IrProperty =
            IrPropertyImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    declaration.isDelegated,
                    mapPropertyDeclaration(declaration.descriptor),
                    declaration.backingField?.transform(this, null) as? IrField,
                    declaration.getter?.transform(this, null) as? IrFunction,
                    declaration.setter?.transform(this, null) as? IrFunction
            )

    override fun visitField(declaration: IrField): IrField =
            IrFieldImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapPropertyDeclaration(declaration.descriptor),
                    declaration.initializer?.transform(this, null) as? IrExpressionBody
            )

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty =
            IrLocalDelegatedPropertyImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapLocalPropertyDeclaration(declaration.descriptor),
                    declaration.delegate.transform(this, null) as IrVariable,
                    declaration.getter.transform(this, null) as IrFunction,
                    declaration.setter?.transform(this, null) as IrFunction?
            )

    override fun visitEnumEntry(declaration: IrEnumEntry): IrEnumEntry =
            IrEnumEntryImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapEnumEntryDeclaration(declaration.descriptor),
                    declaration.correspondingClass?.transform(this, null) as? IrClass,
                    declaration.initializerExpression.transform(this, null)
            )

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrAnonymousInitializer =
            IrAnonymousInitializerImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapClassDeclaration(declaration.descriptor),
                    declaration.body.transform(this, null) as IrBlockBody
            )

    override fun visitVariable(declaration: IrVariable): IrVariable =
            IrVariableImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    mapVariableDeclaration(declaration.descriptor),
                    declaration.initializer?.transform(this, null) as? IrExpression
            )

    override fun visitBody(body: IrBody): IrBody =
            throw IllegalArgumentException("Unsupported body type: $body")

    override fun visitExpressionBody(body: IrExpressionBody): IrExpressionBody =
            IrExpressionBodyImpl(body.expression.transform(this, null))

    override fun visitBlockBody(body: IrBlockBody): IrBlockBody =
            IrBlockBodyImpl(
                    body.startOffset, body.endOffset,
                    body.statements.map { it.transform(this, null) }
            )

    override fun visitSyntheticBody(body: IrSyntheticBody): IrSyntheticBody =
            IrSyntheticBodyImpl(body.startOffset, body.endOffset, body.kind)

    override fun visitExpression(expression: IrExpression): IrExpression =
            throw IllegalArgumentException("Unsupported expression type: $expression")

    override fun <T> visitConst(expression: IrConst<T>): IrConst<T> =
            expression.copy()

    override fun visitVararg(expression: IrVararg): IrVararg =
            IrVarargImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type, expression.varargElementType,
                    expression.elements.map { it.transform(this, null) as IrVarargElement }
            )

    override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement =
            IrSpreadElementImpl(
                    spread.startOffset, spread.endOffset,
                    spread.expression.transform(this, null)
            )

    override fun visitBlock(expression: IrBlock): IrBlock =
            IrBlockImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapStatementOrigin(expression.origin),
                    expression.statements.map { it.transform(this, null) }
            )

    override fun visitComposite(expression: IrComposite): IrComposite =
            IrCompositeImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapStatementOrigin(expression.origin),
                    expression.statements.map { it.transform(this, null) }
            )

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrStringConcatenation =
            IrStringConcatenationImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.arguments.map { it.transform(this, null) }
            )

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrGetObjectValue =
            IrGetObjectValueImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapClassReference(expression.descriptor)
            )

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrGetEnumValue =
            IrGetEnumValueImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapClassReference(expression.descriptor)
            )

    override fun visitGetValue(expression: IrGetValue): IrGetValue =
            IrGetValueImpl(
                    expression.startOffset, expression.endOffset,
                    mapValueReference(expression.descriptor),
                    mapStatementOrigin(expression.origin)
            )

    override fun visitSetVariable(expression: IrSetVariable): IrSetVariable =
            IrSetVariableImpl(
                    expression.startOffset, expression.endOffset,
                    mapVariableReference(expression.descriptor),
                    expression.value.transform(this, null),
                    mapStatementOrigin(expression.origin)
            )

    override fun visitGetField(expression: IrGetField): IrGetField =
            IrGetFieldImpl(
                    expression.startOffset, expression.endOffset,
                    mapPropertyReference(expression.descriptor),
                    expression.receiver?.transform(this, null),
                    mapStatementOrigin(expression.origin),
                    mapSuperQualifier(expression.superQualifier)
            )

    override fun visitSetField(expression: IrSetField): IrSetField =
            IrSetFieldImpl(
                    expression.startOffset, expression.endOffset,
                    mapPropertyReference(expression.descriptor),
                    expression.receiver?.transform(this, null),
                    expression.value.transform(this, null),
                    mapStatementOrigin(expression.origin),
                    mapSuperQualifier(expression.superQualifier)
            )

    override fun visitCall(expression: IrCall): IrCall =
            shallowCopyCall(expression).transformValueArguments(expression)

    protected fun shallowCopyCall(expression: IrCall) =
            when (expression) {
                is IrCallWithShallowCopy ->
                    expression.shallowCopy(
                            mapStatementOrigin(expression.origin),
                            mapCallee(expression.descriptor),
                            mapSuperQualifier(expression.superQualifier)
                    )
                else ->
                    IrCallImpl(
                            expression.startOffset, expression.endOffset,
                            expression.type,
                            mapCallee(expression.descriptor),
                            expression.getTypeArgumentsMap(),
                            mapStatementOrigin(expression.origin),
                            mapSuperQualifier(expression.superQualifier)
                    )
            }

    protected fun <T : IrMemberAccessExpression> T.transformValueArguments(original: IrMemberAccessExpression): T =
            apply {
                dispatchReceiver = original.dispatchReceiver?.transform(this@DeepCopyIrTree, null)
                extensionReceiver = original.extensionReceiver?.transform(this@DeepCopyIrTree, null)
                mapValueParameters { valueParameter ->
                    original.getValueArgument(valueParameter)?.transform(this@DeepCopyIrTree, null)
                }
                Unit
            }

    protected fun IrMemberAccessExpression.getTypeArgumentsMap(): Map<TypeParameterDescriptor, KotlinType>? {
        if (this is IrMemberAccessExpressionBase) return typeArguments

        val typeParameters = descriptor.original.typeParameters
        return if (typeParameters.isEmpty())
            null
        else
            typeParameters.associateBy({ it }, { getTypeArgument(it)!! })
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrDelegatingConstructorCall =
            IrDelegatingConstructorCallImpl(
                    expression.startOffset, expression.endOffset,
                    mapDelegatedConstructorCallee(expression.descriptor),
                    expression.getTypeArgumentsMap()
            ).transformValueArguments(expression)

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrEnumConstructorCall =
            IrEnumConstructorCallImpl(
                    expression.startOffset, expression.endOffset,
                    mapEnumConstructorCallee(expression.descriptor)
            ).transformValueArguments(expression)

    override fun visitGetClass(expression: IrGetClass): IrGetClass =
            IrGetClassImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.argument.transform(this, null)
            )

    override fun visitCallableReference(expression: IrCallableReference): IrCallableReference =
            IrCallableReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapCallableReference(expression.descriptor),
                    expression.getTypeArgumentsMap(),
                    mapStatementOrigin(expression.origin)
            ).transformValueArguments(expression)

    override fun visitClassReference(expression: IrClassReference): IrClassReference =
            IrClassReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapClassifierReference(expression.descriptor)
            )

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrInstanceInitializerCall =
            IrInstanceInitializerCallImpl(
                    expression.startOffset, expression.endOffset,
                    mapClassReference(expression.classDescriptor)
            )

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall =
            IrTypeOperatorCallImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.operator,
                    expression.typeOperand,
                    expression.argument.transform(this, null)
            )

    override fun visitWhen(expression: IrWhen): IrWhen =
            IrWhenImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapStatementOrigin(expression.origin),
                    expression.branches.map { it.transform(this, null) }
            )

    override fun visitBranch(branch: IrBranch): IrBranch =
            IrBranchImpl(
                    branch.startOffset, branch.endOffset,
                    branch.condition.transform(this, null),
                    branch.result.transform(this, null)
            )

    override fun visitElseBranch(branch: IrElseBranch): IrElseBranch =
            IrElseBranchImpl(
                    branch.startOffset, branch.endOffset,
                    branch.condition.transform(this, null),
                    branch.result.transform(this, null)
            )

    private val transformedLoops = HashMap<IrLoop, IrLoop>()

    private fun getTransformedLoop(irLoop: IrLoop): IrLoop =
            transformedLoops.getOrElse(irLoop) { getNonTransformedLoop(irLoop) }

    protected open fun getNonTransformedLoop(irLoop: IrLoop): IrLoop =
            throw AssertionError("Outer loop was not transformed: ${irLoop.render()}")

    override fun visitWhileLoop(loop: IrWhileLoop): IrWhileLoop {
        val newLoop = IrWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, mapStatementOrigin(loop.origin))
        transformedLoops[loop] = newLoop
        newLoop.label = loop.label
        newLoop.condition = loop.condition.transform(this, null)
        newLoop.body = loop.body?.transform(this, null)
        return newLoop
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrDoWhileLoop {
        val newLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, mapStatementOrigin(loop.origin))
        transformedLoops[loop] = newLoop
        newLoop.label = loop.label
        newLoop.condition = loop.condition.transform(this, null)
        newLoop.body = loop.body?.transform(this, null)
        return newLoop
    }

    override fun visitBreak(jump: IrBreak): IrBreak =
            IrBreakImpl(
                    jump.startOffset, jump.endOffset,
                    jump.type,
                    getTransformedLoop(jump.loop)
            ).apply { label = jump.label }

    override fun visitContinue(jump: IrContinue): IrContinue =
            IrContinueImpl(
                    jump.startOffset, jump.endOffset,
                    jump.type,
                    getTransformedLoop(jump.loop)
            ).apply { label = jump.label }

    override fun visitTry(aTry: IrTry): IrTry =
            IrTryImpl(
                    aTry.startOffset, aTry.endOffset,
                    aTry.type,
                    aTry.tryResult.transform(this, null),
                    aTry.catches.map { it.transform(this, null) },
                    aTry.finallyExpression?.transform(this, null)
            )

    override fun visitCatch(aCatch: IrCatch): IrCatch =
            IrCatchImpl(
                    aCatch.startOffset, aCatch.endOffset,
                    mapCatchParameterDeclaration(aCatch.parameter),
                    aCatch.result.transform(this, null)
            )

    override fun visitReturn(expression: IrReturn): IrReturn =
            IrReturnImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapReturnTarget(expression.returnTarget),
                    expression.value.transform(this, null)
            )

    override fun visitThrow(expression: IrThrow): IrThrow =
            IrThrowImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.value.transform(this, null)
            )

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration): IrErrorDeclaration =
            IrErrorDeclarationImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapErrorDeclaration(declaration.descriptor)
            )

    override fun visitErrorExpression(expression: IrErrorExpression): IrErrorExpression =
            IrErrorExpressionImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.description
            )

    override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrErrorCallExpression =
            IrErrorCallExpressionImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.description
            ).apply {
                explicitReceiver = expression.explicitReceiver?.transform(this@DeepCopyIrTree, null)
                expression.arguments.mapTo(arguments) { it.transform(this@DeepCopyIrTree, null) }
            }
}