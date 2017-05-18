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

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

inline fun <reified T : IrElement> T.deepCopyWithSymbols(): T {
    val remapper = DeepCopySymbolsRemapper()
    acceptVoid(remapper)
    return transform(DeepCopyIrTreeWithSymbols(remapper), null) as T
}


open class DeepCopyIrTreeWithSymbols(private val symbolRemapper: SymbolRemapper) : IrElementTransformerVoid() {
    private fun mapDeclarationOrigin(origin: IrDeclarationOrigin) = origin
    private fun mapStatementOrigin(origin: IrStatementOrigin?) = origin

    private inline fun <reified T : IrElement> T.transform() =
            transform(this@DeepCopyIrTreeWithSymbols, null) as T

    private inline fun <reified T : IrElement> List<T>.transform() =
            map { it.transform() }

    private inline fun <reified T : IrElement> List<T>.transformTo(destination: MutableList<T>) =
            mapTo(destination) { it.transform() }

    private fun <T : IrDeclarationContainer> T.transformDeclarationsTo(destination: T) =
            declarations.transformTo(destination.declarations)

    override fun visitElement(element: IrElement): IrElement =
            throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment =
            IrModuleFragmentImpl(
                    declaration.descriptor,
                    declaration.irBuiltins,
                    declaration.files.transform()
            )

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): IrExternalPackageFragment =
            IrExternalPackageFragmentImpl(
                    symbolRemapper.getDeclaredExternalPackageFragment(declaration.symbol)
            ).apply {
                declaration.transformDeclarationsTo(this)
            }

    override fun visitFile(declaration: IrFile): IrFile =
            IrFileImpl(
                    declaration.fileEntry,
                    symbolRemapper.getDeclaredFile(declaration.symbol)
            ).apply {
                fileAnnotations.addAll(declaration.fileAnnotations)
                declaration.transformDeclarationsTo(this)
            }

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement =
            throw IllegalArgumentException("Unsupported declaration type: $declaration")

    override fun visitClass(declaration: IrClass): IrClass =
            IrClassImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    symbolRemapper.getDeclaredClass(declaration.symbol)
            ).apply {
                thisReceiver = declaration.thisReceiver?.transform()
                declaration.typeParameters.transformTo(typeParameters)
                declaration.transformDeclarationsTo(this)
            }

    override fun visitTypeAlias(declaration: IrTypeAlias): IrTypeAlias =
            IrTypeAliasImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    declaration.descriptor
            )

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
            IrFunctionImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    symbolRemapper.getDeclaredFunction(declaration.symbol)
            ).apply {
                transformFunctionChildren(declaration)
            }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor =
            IrConstructorImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    symbolRemapper.getDeclaredConstructor(declaration.symbol)
            ).apply {
                transformFunctionChildren(declaration)
            }

    private fun <T : IrFunction> T.transformFunctionChildren(declaration: T): T =
            apply {
                declaration.typeParameters.transformTo(typeParameters)
                dispatchReceiverParameter = declaration.dispatchReceiverParameter?.transform()
                extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                declaration.valueParameters.transformTo(valueParameters)
                body = declaration.body?.transform()
            }

    override fun visitProperty(declaration: IrProperty): IrProperty =
            IrPropertyImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    declaration.isDelegated,
                    declaration.descriptor,
                    declaration.backingField?.transform(),
                    declaration.getter?.transform(),
                    declaration.setter?.transform()
            )

    override fun visitField(declaration: IrField): IrField =
            IrFieldImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    symbolRemapper.getDeclaredField(declaration.symbol)
            ).apply {
                initializer = declaration.initializer?.transform()
            }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty =
            IrLocalDelegatedPropertyImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    declaration.descriptor,
                    declaration.delegate.transform(),
                    declaration.getter.transform(),
                    declaration.setter?.transform()
            )

    override fun visitEnumEntry(declaration: IrEnumEntry): IrEnumEntry =
            IrEnumEntryImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    symbolRemapper.getDeclaredEnumEntry(declaration.symbol)
            ).apply {
                correspondingClass = declaration.correspondingClass?.transform()
                initializerExpression = declaration.initializerExpression?.transform()
            }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrAnonymousInitializer =
            IrAnonymousInitializerImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    IrAnonymousInitializerSymbolImpl(declaration.descriptor)
            ).apply {
                body = declaration.body.transform()
            }

    override fun visitVariable(declaration: IrVariable): IrVariable =
            IrVariableImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    symbolRemapper.getDeclaredVariable(declaration.symbol)
            ).apply {
                initializer = declaration.initializer?.transform()
            }

    override fun visitTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
            IrTypeParameterImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    symbolRemapper.getDeclaredTypeParameter(declaration.symbol)
            )

    override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter =
            IrValueParameterImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    symbolRemapper.getDeclaredValueParameter(declaration.symbol)
            ).apply {
                defaultValue = declaration.defaultValue?.transform()
            }

    override fun visitBody(body: IrBody): IrBody =
            throw IllegalArgumentException("Unsupported body type: $body")

    override fun visitExpressionBody(body: IrExpressionBody): IrExpressionBody =
            IrExpressionBodyImpl(body.expression.transform())

    override fun visitBlockBody(body: IrBlockBody): IrBlockBody =
            IrBlockBodyImpl(
                    body.startOffset, body.endOffset,
                    body.statements.map { it.transform() }
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
                    expression.elements.transform()
            )

    override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement =
            IrSpreadElementImpl(
                    spread.startOffset, spread.endOffset,
                    spread.expression.transform()
            )

    override fun visitBlock(expression: IrBlock): IrBlock =
            IrBlockImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapStatementOrigin(expression.origin),
                    expression.statements.map { it.transform() }
            )

    override fun visitComposite(expression: IrComposite): IrComposite =
            IrCompositeImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapStatementOrigin(expression.origin),
                    expression.statements.map { it.transform() }
            )

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrStringConcatenation =
            IrStringConcatenationImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.arguments.map { it.transform() }
            )

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrGetObjectValue =
            IrGetObjectValueImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    symbolRemapper.getReferencedClass(expression.symbol)
            )

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrGetEnumValue =
            IrGetEnumValueImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    symbolRemapper.getReferencedEnumEntry(expression.symbol)
            )

    override fun visitGetValue(expression: IrGetValue): IrGetValue =
            IrGetValueImpl(
                    expression.startOffset, expression.endOffset,
                    symbolRemapper.getReferencedValue(expression.symbol),
                    mapStatementOrigin(expression.origin)
            )

    override fun visitSetVariable(expression: IrSetVariable): IrSetVariable =
            IrSetVariableImpl(
                    expression.startOffset, expression.endOffset,
                    symbolRemapper.getReferencedVariable(expression.symbol),
                    expression.value.transform(),
                    mapStatementOrigin(expression.origin)
            )

    override fun visitGetField(expression: IrGetField): IrGetField =
            IrGetFieldImpl(
                    expression.startOffset, expression.endOffset,
                    symbolRemapper.getReferencedField(expression.symbol),
                    expression.receiver?.transform(),
                    mapStatementOrigin(expression.origin),
                    symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
            )

    override fun visitSetField(expression: IrSetField): IrSetField =
            IrSetFieldImpl(
                    expression.startOffset, expression.endOffset,
                    symbolRemapper.getReferencedField(expression.symbol),
                    expression.receiver?.transform(),
                    expression.value.transform(),
                    mapStatementOrigin(expression.origin),
                    symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
            )

    override fun visitCall(expression: IrCall): IrCall =
            shallowCopyCall(expression).transformValueArguments(expression)

    private fun shallowCopyCall(expression: IrCall) =
            when (expression) {
                is IrCallWithShallowCopy ->
                    expression.shallowCopy(
                            mapStatementOrigin(expression.origin),
                            symbolRemapper.getReferencedFunction(expression.symbol),
                            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
                    )
                else ->
                    IrCallImpl(
                            expression.startOffset, expression.endOffset,
                            expression.type,
                            symbolRemapper.getReferencedFunction(expression.symbol),
                            expression.descriptor, // TODO substitute referenced descriptor
                            expression.getTypeArgumentsMap(),
                            mapStatementOrigin(expression.origin),
                            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
                    )
            }

    private fun <T : IrMemberAccessExpression> T.transformReceiverArguments(original: T): T =
            apply {
                dispatchReceiver = original.dispatchReceiver?.transform()
                extensionReceiver = original.extensionReceiver?.transform()
            }

    private fun <T : IrMemberAccessExpression> T.transformValueArguments(original: T): T =
            apply {
                transformReceiverArguments(original)
                mapValueParameters { valueParameter ->
                    original.getValueArgument(valueParameter)?.transform()
                }
            }

    private fun IrMemberAccessExpression.getTypeArgumentsMap(): Map<TypeParameterDescriptor, KotlinType>? {
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
                    symbolRemapper.getReferencedConstructor(expression.symbol),
                    expression.descriptor,
                    expression.getTypeArgumentsMap()
            ).transformValueArguments(expression)

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrEnumConstructorCall =
            IrEnumConstructorCallImpl(
                    expression.startOffset, expression.endOffset,
                    symbolRemapper.getReferencedConstructor(expression.symbol)
            ).transformValueArguments(expression)

    override fun visitGetClass(expression: IrGetClass): IrGetClass =
            IrGetClassImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.argument.transform()
            )

    override fun visitFunctionReference(expression: IrFunctionReference): IrFunctionReference =
            IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    symbolRemapper.getReferencedFunction(expression.symbol),
                    expression.descriptor, // TODO substitute referenced descriptor
                    expression.getTypeArgumentsMap(),
                    mapStatementOrigin(expression.origin)
            ).transformValueArguments(expression)

    override fun visitPropertyReference(expression: IrPropertyReference): IrPropertyReference =
            IrPropertyReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.descriptor,
                    expression.field?.let { symbolRemapper.getReferencedField(it) },
                    expression.getter?.let { symbolRemapper.getReferencedFunction(it) },
                    expression.setter?.let { symbolRemapper.getReferencedFunction(it) },
                    expression.getTypeArgumentsMap(),
                    mapStatementOrigin(expression.origin)
            ).transformReceiverArguments(expression)

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrLocalDelegatedPropertyReference =
            IrLocalDelegatedPropertyReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.descriptor,
                    symbolRemapper.getReferencedVariable(expression.delegate),
                    symbolRemapper.getReferencedFunction(expression.getter),
                    expression.setter?.let { symbolRemapper.getReferencedFunction(it) },
                    mapStatementOrigin(expression.origin)
            )

    override fun visitClassReference(expression: IrClassReference): IrClassReference =
            IrClassReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    symbolRemapper.getReferencedClassifier(expression.symbol)
            )

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrInstanceInitializerCall =
            IrInstanceInitializerCallImpl(
                    expression.startOffset, expression.endOffset,
                    symbolRemapper.getReferencedClass(expression.classSymbol)
            )

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall =
            IrTypeOperatorCallImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.operator,
                    expression.typeOperand,
                    expression.argument.transform()
            )

    override fun visitWhen(expression: IrWhen): IrWhen =
            IrWhenImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    mapStatementOrigin(expression.origin),
                    expression.branches.map { it.transform() }
            )

    override fun visitBranch(branch: IrBranch): IrBranch =
            IrBranchImpl(
                    branch.startOffset, branch.endOffset,
                    branch.condition.transform(),
                    branch.result.transform()
            )

    override fun visitElseBranch(branch: IrElseBranch): IrElseBranch =
            IrElseBranchImpl(
                    branch.startOffset, branch.endOffset,
                    branch.condition.transform(),
                    branch.result.transform()
            )

    private val transformedLoops = HashMap<IrLoop, IrLoop>()

    private fun getTransformedLoop(irLoop: IrLoop): IrLoop =
            transformedLoops.getOrElse(irLoop) { getNonTransformedLoop(irLoop) }

    protected open fun getNonTransformedLoop(irLoop: IrLoop): IrLoop =
            throw AssertionError("Outer loop was not transformed: ${irLoop.render()}")

    override fun visitWhileLoop(loop: IrWhileLoop): IrWhileLoop =
            IrWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, mapStatementOrigin(loop.origin)).also { newLoop ->
                transformedLoops[loop] = newLoop
                newLoop.label = loop.label
                newLoop.condition = loop.condition.transform()
                newLoop.body = loop.body?.transform()
            }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrDoWhileLoop =
            IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, mapStatementOrigin(loop.origin)).also { newLoop ->
                transformedLoops[loop] = newLoop
                newLoop.label = loop.label
                newLoop.condition = loop.condition.transform()
                newLoop.body = loop.body?.transform()
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
                    aTry.tryResult.transform(),
                    aTry.catches.map { it.transform() },
                    aTry.finallyExpression?.transform()
            )

    override fun visitCatch(aCatch: IrCatch): IrCatch =
            IrCatchImpl(
                    aCatch.startOffset, aCatch.endOffset,
                    aCatch.catchParameter.transform(),
                    aCatch.result.transform()
            )

    override fun visitReturn(expression: IrReturn): IrReturn =
            IrReturnImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    symbolRemapper.getReferencedFunction(expression.returnTargetSymbol),
                    expression.value.transform()
            )

    override fun visitThrow(expression: IrThrow): IrThrow =
            IrThrowImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.value.transform()
            )

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration): IrErrorDeclaration =
            IrErrorDeclarationImpl(declaration.startOffset, declaration.endOffset, declaration.descriptor)

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
                explicitReceiver = expression.explicitReceiver?.transform()
                expression.arguments.transformTo(arguments)
            }
}


