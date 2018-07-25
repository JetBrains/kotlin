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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import java.util.*

inline fun <reified T : IrElement> T.deepCopyWithSymbols(initialParent: IrDeclarationParent? = null): T {
    val symbolRemapper = DeepCopySymbolRemapper()
    acceptVoid(symbolRemapper)
    val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
    return transform(DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper), null).patchDeclarationParents(initialParent) as T
}


open class DeepCopyIrTreeWithSymbols(
    private val symbolRemapper: SymbolRemapper,
    private val typeRemapper: TypeRemapper
) : IrElementTransformerVoid() {

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

    private fun IrType.remapType() = typeRemapper.remapType(this)

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
            transformAnnotations(declaration)
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
            transformAnnotations(declaration)
            copyTypeParametersFrom(declaration)
            declaration.superTypes.mapTo(superTypes) {
                it.remapType()
            }
            thisReceiver = declaration.thisReceiver?.transform()
            declaration.transformDeclarationsTo(this)
        }

    override fun visitTypeAlias(declaration: IrTypeAlias): IrTypeAlias =
        IrTypeAliasImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            declaration.descriptor
        ).apply {
            transformAnnotations(declaration)
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        IrFunctionImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            symbolRemapper.getDeclaredFunction(declaration.symbol)
        ).apply {
            declaration.overriddenSymbols.mapTo(overriddenSymbols) {
                symbolRemapper.getReferencedFunction(it) as IrSimpleFunctionSymbol
            }
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
            transformAnnotations(declaration)
            copyTypeParametersFrom(declaration)
            typeRemapper.withinScope(this) {
                dispatchReceiverParameter = declaration.dispatchReceiverParameter?.transform()
                extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                returnType = typeRemapper.remapType(declaration.returnType)
                declaration.valueParameters.transformTo(valueParameters)
                body = declaration.body?.transform()
            }
        }

    private fun IrAnnotationContainer.transformAnnotations(declaration: IrAnnotationContainer) {
        declaration.annotations.transformTo(annotations)
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
        ).apply {
            transformAnnotations(declaration)
        }

    override fun visitField(declaration: IrField): IrField =
        IrFieldImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            symbolRemapper.getDeclaredField(declaration.symbol),
            declaration.type.remapType()
        ).apply {
            transformAnnotations(declaration)
            declaration.overriddenSymbols.mapTo(overriddenSymbols) {
                symbolRemapper.getReferencedField(it)
            }
            initializer = declaration.initializer?.transform()
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty =
        IrLocalDelegatedPropertyImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            declaration.descriptor,
            declaration.type.remapType(),
            declaration.delegate.transform(),
            declaration.getter.transform(),
            declaration.setter?.transform()
        ).apply {
            transformAnnotations(declaration)
        }

    override fun visitEnumEntry(declaration: IrEnumEntry): IrEnumEntry =
        IrEnumEntryImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            symbolRemapper.getDeclaredEnumEntry(declaration.symbol)
        ).apply {
            transformAnnotations(declaration)
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
            symbolRemapper.getDeclaredVariable(declaration.symbol),
            declaration.type.remapType()
        ).apply {
            transformAnnotations(declaration)
            initializer = declaration.initializer?.transform()
        }

    override fun visitTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
        copyTypeParameter(declaration).apply {
            // TODO type parameter scopes?
            declaration.superTypes.mapTo(superTypes) { it.remapType() }
        }

    private fun copyTypeParameter(declaration: IrTypeParameter) =
        IrTypeParameterImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            symbolRemapper.getDeclaredTypeParameter(declaration.symbol)
        ).apply {
            transformAnnotations(declaration)
        }

    private fun IrTypeParametersContainer.copyTypeParametersFrom(other: IrTypeParametersContainer) {
        other.typeParameters.mapTo(this.typeParameters) {
            copyTypeParameter(it)
        }

        typeRemapper.withinScope(this) {
            for ((thisTypeParameter, otherTypeParameter) in this.typeParameters.zip(other.typeParameters)) {
                otherTypeParameter.superTypes.mapTo(thisTypeParameter.superTypes) {
                    typeRemapper.remapType(it)
                }
            }
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter =
        IrValueParameterImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            symbolRemapper.getDeclaredValueParameter(declaration.symbol),
            declaration.type.remapType(),
            declaration.varargElementType?.remapType()
        ).apply {
            transformAnnotations(declaration)
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
            expression.type.remapType(), expression.varargElementType.remapType(),
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
            expression.type.remapType(),
            mapStatementOrigin(expression.origin),
            expression.statements.map { it.transform() }
        )

    override fun visitComposite(expression: IrComposite): IrComposite =
        IrCompositeImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            mapStatementOrigin(expression.origin),
            expression.statements.map { it.transform() }
        )

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrStringConcatenation =
        IrStringConcatenationImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.arguments.map { it.transform() }
        )

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrGetObjectValue =
        IrGetObjectValueImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedClass(expression.symbol)
        )

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrGetEnumValue =
        IrGetEnumValueImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedEnumEntry(expression.symbol)
        )

    override fun visitGetValue(expression: IrGetValue): IrGetValue =
        IrGetValueImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedValue(expression.symbol),
            mapStatementOrigin(expression.origin)
        )

    override fun visitSetVariable(expression: IrSetVariable): IrSetVariable =
        IrSetVariableImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedVariable(expression.symbol),
            expression.value.transform(),
            mapStatementOrigin(expression.origin)
        )

    override fun visitGetField(expression: IrGetField): IrGetField =
        IrGetFieldImpl(
            expression.startOffset, expression.endOffset,
            symbolRemapper.getReferencedField(expression.symbol),
            expression.type.remapType(),
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
            expression.type.remapType(),
            mapStatementOrigin(expression.origin),
            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
        )

    override fun visitCall(expression: IrCall): IrCall =
        shallowCopyCall(expression).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }

    private fun IrMemberAccessExpression.copyRemappedTypeArgumentsFrom(other: IrMemberAccessExpression) {
        assert(typeArgumentsCount == other.typeArgumentsCount) {
            "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
        }
        for (i in 0 until typeArgumentsCount) {
            putTypeArgument(i, other.getTypeArgument(i)?.remapType())
        }
    }

    private fun shallowCopyCall(expression: IrCall) =
        when (expression) {
            is IrCallWithShallowCopy ->
                expression.shallowCopy(
                    mapStatementOrigin(expression.origin),
                    symbolRemapper.getReferencedFunction(expression.symbol),
                    symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
                )
            else -> {
                val newCallee = symbolRemapper.getReferencedFunction(expression.symbol)
                IrCallImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type.remapType(),
                    newCallee,
                    expression.descriptor, // TODO substitute referenced descriptor
                    expression.typeArgumentsCount,
                    mapStatementOrigin(expression.origin),
                    symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
                ).apply {
                    copyRemappedTypeArgumentsFrom(expression)
                }
            }
        }

    private fun <T : IrMemberAccessExpression> T.transformReceiverArguments(original: T): T =
        apply {
            dispatchReceiver = original.dispatchReceiver?.transform()
            extensionReceiver = original.extensionReceiver?.transform()
        }

    private fun <T : IrMemberAccessExpression> T.transformValueArguments(original: T) {
        transformReceiverArguments(original)
        mapValueParameters { valueParameter ->
            original.getValueArgument(valueParameter)?.transform()
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrDelegatingConstructorCall {
        val newConstructor = symbolRemapper.getReferencedConstructor(expression.symbol)
        return IrDelegatingConstructorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newConstructor,
            expression.descriptor,
            expression.typeArgumentsCount
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrEnumConstructorCall {
        val newConstructor = symbolRemapper.getReferencedConstructor(expression.symbol)
        return IrEnumConstructorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newConstructor,
            expression.typeArgumentsCount
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }
    }

    override fun visitGetClass(expression: IrGetClass): IrGetClass =
        IrGetClassImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.argument.transform()
        )

    override fun visitFunctionReference(expression: IrFunctionReference): IrFunctionReference =
        IrFunctionReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedFunction(expression.symbol),
            expression.descriptor, // TODO substitute referenced descriptor
            expression.typeArgumentsCount,
            mapStatementOrigin(expression.origin)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }

    override fun visitPropertyReference(expression: IrPropertyReference): IrPropertyReference =
        IrPropertyReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.descriptor,
            expression.typeArgumentsCount,
            expression.field?.let { symbolRemapper.getReferencedField(it) },
            expression.getter?.let { symbolRemapper.getReferencedSimpleFunction(it) },
            expression.setter?.let { symbolRemapper.getReferencedSimpleFunction(it) },
            mapStatementOrigin(expression.origin)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformReceiverArguments(expression)
        }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrLocalDelegatedPropertyReference =
        IrLocalDelegatedPropertyReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.descriptor,
            symbolRemapper.getReferencedVariable(expression.delegate),
            symbolRemapper.getReferencedSimpleFunction(expression.getter),
            expression.setter?.let { symbolRemapper.getReferencedSimpleFunction(it) },
            mapStatementOrigin(expression.origin)
        )

    override fun visitClassReference(expression: IrClassReference): IrClassReference =
        IrClassReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedClassifier(expression.symbol),
            expression.classType
        )

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrInstanceInitializerCall =
        IrInstanceInitializerCallImpl(
            expression.startOffset, expression.endOffset,
            symbolRemapper.getReferencedClass(expression.classSymbol),
            expression.type.remapType()
        )

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall =
        IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.operator,
            expression.typeOperand,
            symbolRemapper.getReferencedClassifier(expression.typeOperandClassifier),
            expression.argument.transform()
        )

    override fun visitWhen(expression: IrWhen): IrWhen =
        IrWhenImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
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
            expression.type.remapType(),
            symbolRemapper.getReferencedReturnTarget(expression.returnTargetSymbol),
            expression.value.transform()
        )

    private fun SymbolRemapper.getReferencedReturnTarget(returnTarget: IrReturnTargetSymbol) =
        when (returnTarget) {
            is IrFunctionSymbol -> getReferencedFunction(returnTarget)
            is IrReturnableBlockSymbol -> getReferencedReturnableBlock(returnTarget)
            else -> throw AssertionError("Unexpected return target: ${returnTarget.javaClass} $returnTarget")
        }

    override fun visitThrow(expression: IrThrow): IrThrow =
        IrThrowImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.value.transform()
        )

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration): IrErrorDeclaration =
        IrErrorDeclarationImpl(declaration.startOffset, declaration.endOffset, declaration.descriptor)

    override fun visitErrorExpression(expression: IrErrorExpression): IrErrorExpression =
        IrErrorExpressionImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.description
        )

    override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrErrorCallExpression =
        IrErrorCallExpressionImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.description
        ).apply {
            explicitReceiver = expression.explicitReceiver?.transform()
            expression.arguments.transformTo(arguments)
        }
}


