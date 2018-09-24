/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

inline fun <reified T : IrElement> T.deepCopyOld(): T =
    transform(DeepCopyIrTree(), null).patchDeclarationParents() as T

@Deprecated("Creates unbound symbols")
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
    protected open fun mapErrorDeclaration(descriptor: DeclarationDescriptor) = descriptor

    protected open fun mapSuperQualifier(qualifier: ClassDescriptor?) = qualifier
    protected open fun mapClassReference(descriptor: ClassDescriptor) = descriptor
    protected open fun mapValueReference(descriptor: ValueDescriptor) = descriptor
    protected open fun mapVariableReference(descriptor: VariableDescriptor) = descriptor
    protected open fun mapPropertyReference(descriptor: PropertyDescriptor) = descriptor
    protected open fun mapCallee(descriptor: FunctionDescriptor) = descriptor
    protected open fun mapDelegatedConstructorCallee(descriptor: ClassConstructorDescriptor) = descriptor
    protected open fun mapEnumConstructorCallee(descriptor: ClassConstructorDescriptor) = descriptor
    protected open fun mapLocalPropertyReference(descriptor: VariableDescriptorWithAccessors) = descriptor
    protected open fun mapClassifierReference(descriptor: ClassifierDescriptor) = descriptor
    protected open fun mapReturnTarget(descriptor: FunctionDescriptor) = mapCallee(descriptor)

    private inline fun <reified T : IrElement> T.transform() = transform(this@DeepCopyIrTree, null) as T

    override fun visitElement(element: IrElement): IrElement =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment =
        IrModuleFragmentImpl(
            mapModuleDescriptor(declaration.descriptor),
            declaration.irBuiltins,
            declaration.files.map { it.transform() }
        )

    override fun visitFile(declaration: IrFile): IrFile =
        IrFileImpl(
            mapFileEntry(declaration.fileEntry),
            mapPackageFragmentDescriptor(declaration.packageFragmentDescriptor),
            declaration.fileAnnotations.toMutableList(),
            declaration.declarations.map { it.transform() }
        ).apply {
            transformAnnotations(declaration)
        }

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement =
        throw IllegalArgumentException("Unsupported declaration type: $declaration")

    override fun visitClass(declaration: IrClass): IrClass =
        IrClassImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            mapClassDeclaration(declaration.descriptor),
            declaration.declarations.map { it.transform() }
        ).apply {
            transformAnnotations(declaration)
            thisReceiver = declaration.thisReceiver?.replaceDescriptor(descriptor.thisAsReceiverParameter)

            transformTypeParameters(declaration, descriptor.declaredTypeParameters)

            superTypes.addAll(declaration.superTypes) // TODO
        }

    override fun visitTypeAlias(declaration: IrTypeAlias): IrTypeAlias =
        IrTypeAliasImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            mapTypeAliasDeclaration(declaration.descriptor)
        ).apply {
            transformAnnotations(declaration)
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrFunction =
        IrFunctionImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            mapFunctionDeclaration(declaration.descriptor),
            declaration.body?.transform()
        ).transformParameters(declaration).apply {
            transformAnnotations(declaration)
            descriptor.overriddenDescriptors.mapIndexedTo(overriddenSymbols) { index, overriddenDescriptor ->
                val oldOverriddenSymbol = declaration.overriddenSymbols.getOrNull(index)
                if (overriddenDescriptor.original == oldOverriddenSymbol?.descriptor?.original)
                    oldOverriddenSymbol
                else
                    IrSimpleFunctionSymbolImpl(overriddenDescriptor.original)
            }
            returnType = declaration.returnType // TODO
        }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor =
        IrConstructorImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            mapConstructorDeclaration(declaration.descriptor),
            declaration.body?.transform()
        ).transformParameters(declaration).apply {
            transformAnnotations(declaration)
            returnType = declaration.returnType // TODO
        }

    protected fun <T : IrTypeParametersContainer> T.transformTypeParameters(
        original: T,
        myTypeParameters: List<TypeParameterDescriptor>
    ): T =
        apply {
            original.typeParameters.mapTo(typeParameters) { originalTypeParameter ->
                copyTypeParameter(originalTypeParameter, myTypeParameters[originalTypeParameter.descriptor.index])
            }
        }

    private fun FunctionDescriptor.getTypeParametersToTransform() =
        when {
            this is PropertyAccessorDescriptor -> correspondingProperty.typeParameters
            else -> typeParameters
        }

    protected fun <T : IrFunction> T.transformParameters(original: T): T =
        apply {
            transformTypeParameters(original, descriptor.getTypeParametersToTransform())
            transformValueParameters(original)
        }

    protected fun <T : IrFunction> T.transformValueParameters(original: T) =
        apply {
            dispatchReceiverParameter =
                    original.dispatchReceiverParameter?.replaceDescriptor(
                        descriptor.dispatchReceiverParameter ?: throw AssertionError("No dispatch receiver in $descriptor")
                    )

            extensionReceiverParameter =
                    original.extensionReceiverParameter?.replaceDescriptor(
                        descriptor.extensionReceiverParameter ?: throw AssertionError("No extension receiver in $descriptor")
                    )

            original.valueParameters.mapIndexedTo(valueParameters) { i, originalValueParameter ->
                originalValueParameter.replaceDescriptor(descriptor.valueParameters[i])
            }
        }

    protected fun IrAnnotationContainer.transformAnnotations(original: IrAnnotationContainer) {
        original.annotations.mapTo(annotations) { it.transform() }
    }

    protected fun copyTypeParameter(
        originalTypeParameter: IrTypeParameter,
        newTypeParameterDescriptor: TypeParameterDescriptor
    ): IrTypeParameterImpl =
        IrTypeParameterImpl(
            originalTypeParameter.startOffset, originalTypeParameter.endOffset,
            mapDeclarationOrigin(originalTypeParameter.origin),
            newTypeParameterDescriptor
        ).apply {
            transformAnnotations(originalTypeParameter)
            superTypes.addAll(originalTypeParameter.superTypes) // TODO
        }

    protected fun createUnboundClassifierSymbol(classifier: ClassifierDescriptor): IrClassifierSymbol =
        when (classifier) {
            is TypeParameterDescriptor -> IrTypeParameterSymbolImpl(classifier)
            is ClassDescriptor -> IrClassSymbolImpl(classifier)
            else -> throw IllegalArgumentException("Unexpected classifier descriptor: $classifier")
        }

    protected fun copyValueParameter(valueParameter: IrValueParameter, newDescriptor: ParameterDescriptor) =
        valueParameter.replaceDescriptor(newDescriptor)

    protected fun IrValueParameter.replaceDescriptor(newDescriptor: ParameterDescriptor) =
        IrValueParameterImpl(
            startOffset, endOffset,
            mapDeclarationOrigin(origin),
            newDescriptor,
            type, // TODO
            varargElementType, // TODO
            defaultValue?.transform()
        ).also { irValueParameter ->
            irValueParameter.transformAnnotations(this)
        }

    // TODO visitTypeParameter
    // TODO visitValueParameter

    override fun visitProperty(declaration: IrProperty): IrProperty =
        IrPropertyImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            declaration.isDelegated,
            mapPropertyDeclaration(declaration.descriptor),
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
            mapPropertyDeclaration(declaration.descriptor),
            declaration.type, // TODO
            declaration.initializer?.transform()
        ).apply {
            transformAnnotations(declaration)
            if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) {
                descriptor.overriddenDescriptors.mapIndexedTo(overriddenSymbols) { index, overriddenDescriptor ->
                    val oldOverriddenSymbol = declaration.overriddenSymbols.getOrNull(index)
                    if (overriddenDescriptor.original == oldOverriddenSymbol?.descriptor?.original)
                        oldOverriddenSymbol
                    else
                        IrFieldSymbolImpl(overriddenDescriptor.original)
                }
            }
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty =
        IrLocalDelegatedPropertyImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            mapLocalPropertyDeclaration(declaration.descriptor),
            declaration.type, // TODO
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
            mapEnumEntryDeclaration(declaration.descriptor),
            declaration.correspondingClass?.transform(),
            declaration.initializerExpression?.transform()
        ).apply {
            transformAnnotations(declaration)
        }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrAnonymousInitializer =
        IrAnonymousInitializerImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            mapClassDeclaration(declaration.descriptor),
            declaration.body.transform()
        )

    override fun visitVariable(declaration: IrVariable): IrVariable =
        IrVariableImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            mapVariableDeclaration(declaration.descriptor),
            declaration.type, // TODO
            declaration.initializer?.transform()
        ).apply {
            transformAnnotations(declaration)
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
            expression.elements.map { it.transform() }
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
            expression.type, // TODO
            mapValueReference(expression.descriptor),
            mapStatementOrigin(expression.origin)
        )

    override fun visitSetVariable(expression: IrSetVariable): IrSetVariable =
        IrSetVariableImpl(
            expression.startOffset, expression.endOffset,
            expression.type, // TODO
            mapVariableReference(expression.descriptor),
            expression.value.transform(),
            mapStatementOrigin(expression.origin)
        )

    override fun visitGetField(expression: IrGetField): IrGetField =
        IrGetFieldImpl(
            expression.startOffset, expression.endOffset,
            mapPropertyReference(expression.descriptor),
            expression.receiver?.transform(),
            expression.type, // TODO
            mapStatementOrigin(expression.origin),
            mapSuperQualifier(expression.superQualifier)
        )

    override fun visitSetField(expression: IrSetField): IrSetField =
        IrSetFieldImpl(
            expression.startOffset, expression.endOffset,
            mapPropertyReference(expression.descriptor),
            expression.receiver?.transform(),
            expression.value.transform(),
            expression.type, // TODO
            mapStatementOrigin(expression.origin),
            mapSuperQualifier(expression.superQualifier)
        )

    override fun visitCall(expression: IrCall): IrCall =
        shallowCopyCall(expression).apply {
            transformValueArguments(expression)
        }

    protected fun shallowCopyCall(expression: IrCall) =
        when (expression) {
            is IrCallWithShallowCopy ->
                expression.shallowCopy(
                    mapStatementOrigin(expression.origin),
                    mapCallee(expression.descriptor),
                    mapSuperQualifier(expression.superQualifier)
                )
            else -> {
                val newCallee = mapCallee(expression.descriptor)
                IrCallImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    newCallee,
                    expression.typeArgumentsCount,
                    mapStatementOrigin(expression.origin),
                    mapSuperQualifier(expression.superQualifier)
                ).apply {
                    copyTypeArgumentsFrom(expression)
                }
            }
        }

    protected fun <T : IrMemberAccessExpression> T.transformValueArguments(original: IrMemberAccessExpression) {
        dispatchReceiver = original.dispatchReceiver?.transform()
        extensionReceiver = original.extensionReceiver?.transform()
        mapValueParameters { valueParameter ->
            original.getValueArgument(valueParameter)?.transform()
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrDelegatingConstructorCall {
        val newCallee = mapDelegatedConstructorCallee(expression.descriptor)
        return IrDelegatingConstructorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type, // TODO
            newCallee,
            expression.typeArgumentsCount
        ).apply {
            copyTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrEnumConstructorCall {
        val oldConstructor = expression.descriptor
        val newConstructor = mapEnumConstructorCallee(oldConstructor)
        return IrEnumConstructorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type, // TODO
            newConstructor,
            expression.typeArgumentsCount
        ).apply {
            copyTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }
    }

    override fun visitGetClass(expression: IrGetClass): IrGetClass =
        IrGetClassImpl(
            expression.startOffset, expression.endOffset,
            expression.type,
            expression.argument.transform()
        )

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val newCallee = mapCallee(expression.descriptor)
        return IrFunctionReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type,
            newCallee,
            expression.typeArgumentsCount,
            mapStatementOrigin(expression.origin)
        ).apply {
            copyTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        val newProperty = mapPropertyReference(expression.descriptor)
        val newFieldSymbol = if (newProperty.getter == null) IrFieldSymbolImpl(newProperty) else null
        val newGetterSymbol = newProperty.getter?.let { IrSimpleFunctionSymbolImpl(it.original) }
        val newSetterSymbol = newProperty.setter?.let { IrSimpleFunctionSymbolImpl(it.original) }
        return IrPropertyReferenceImpl(
            expression.startOffset, expression.endOffset, expression.type,
            newProperty,
            expression.typeArgumentsCount, newFieldSymbol, newGetterSymbol, newSetterSymbol,
            mapStatementOrigin(expression.origin)
        ).apply {
            copyTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
        val newLocalDelegatedProperty = mapLocalPropertyReference(expression.descriptor)
        val newDelegateDescriptor = mapVariableReference(expression.delegate.descriptor)
        val newDelegateSymbol = IrVariableSymbolImpl(newDelegateDescriptor)
        val newGetterSymbol = newLocalDelegatedProperty.getter!!.let { IrSimpleFunctionSymbolImpl(it.original) }
        val newSetterSymbol = newLocalDelegatedProperty.setter?.let { IrSimpleFunctionSymbolImpl(it.original) }
        return IrLocalDelegatedPropertyReferenceImpl(
            expression.startOffset, expression.endOffset, expression.type,
            newLocalDelegatedProperty,
            newDelegateSymbol, newGetterSymbol, newSetterSymbol,
            mapStatementOrigin(expression.origin)
        )
    }

    override fun visitClassReference(expression: IrClassReference): IrClassReference =
        IrClassReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type,
            mapClassifierReference(expression.descriptor),
            expression.classType
        )

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrInstanceInitializerCall =
        IrInstanceInitializerCallImpl(
            expression.startOffset, expression.endOffset,
            mapClassReference(expression.classDescriptor),
            expression.type // TODO
        )

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall =
        IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type,
            expression.operator,
            expression.typeOperand,
            run {
                val oldTypeDescriptor = expression.typeOperandClassifier.descriptor
                val newTypeDescriptor = mapClassifierReference(oldTypeDescriptor)
                if (newTypeDescriptor == oldTypeDescriptor)
                    expression.typeOperandClassifier
                else
                    createUnboundClassifierSymbol(newTypeDescriptor)
            },
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

    override fun visitWhileLoop(loop: IrWhileLoop): IrWhileLoop {
        val newLoop = IrWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, mapStatementOrigin(loop.origin))
        transformedLoops[loop] = newLoop
        newLoop.label = loop.label
        newLoop.condition = loop.condition.transform()
        newLoop.body = loop.body?.transform()
        return newLoop
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrDoWhileLoop {
        val newLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, mapStatementOrigin(loop.origin))
        transformedLoops[loop] = newLoop
        newLoop.label = loop.label
        newLoop.condition = loop.condition.transform()
        newLoop.body = loop.body?.transform()
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
            mapReturnTarget(expression.returnTarget),
            expression.value.transform()
        )

    override fun visitThrow(expression: IrThrow): IrThrow =
        IrThrowImpl(
            expression.startOffset, expression.endOffset,
            expression.type,
            expression.value.transform()
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
            explicitReceiver = expression.explicitReceiver?.transform()
            expression.arguments.mapTo(arguments) { it.transform() }
        }

}