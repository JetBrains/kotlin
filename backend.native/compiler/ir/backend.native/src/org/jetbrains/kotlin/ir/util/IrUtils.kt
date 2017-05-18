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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
internal fun IrMemberAccessExpression.getArguments(): List<Pair<ParameterDescriptor, IrExpression>> {
    val res = mutableListOf<Pair<ParameterDescriptor, IrExpression>>()
    val descriptor = descriptor

    // TODO: ensure the order below corresponds to the one defined in Kotlin specs.

    dispatchReceiver?.let {
        res += (descriptor.dispatchReceiverParameter!! to it)
    }

    extensionReceiver?.let {
        res += (descriptor.extensionReceiverParameter!! to it)
    }

    descriptor.valueParameters.forEach {
        val arg = getValueArgument(it.index)
        if (arg != null) {
            res += (it to arg)
        }
    }

    return res
}

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
internal fun IrFunctionAccessExpression.getArgumentsWithSymbols(): List<Pair<IrValueParameterSymbol, IrExpression>> {
    val res = mutableListOf<Pair<IrValueParameterSymbol, IrExpression>>()
    val irFunction = symbol.owner as IrFunction

    dispatchReceiver?.let {
        res += (irFunction.dispatchReceiverParameter!!.symbol to it)
    }

    extensionReceiver?.let {
        res += (irFunction.extensionReceiverParameter!!.symbol to it)
    }

    irFunction.valueParameters.forEach {
        val arg = getValueArgument(it.descriptor as ValueParameterDescriptor)
        if (arg != null) {
            res += (it.symbol to arg)
        }
    }

    return res
}

/**
 * Sets arguments that are specified by given mapping of parameters.
 */
internal fun IrMemberAccessExpression.addArguments(args: Map<ParameterDescriptor, IrExpression>) {
    descriptor.dispatchReceiverParameter?.let {
        val arg = args[it]
        if (arg != null) {
            this.dispatchReceiver = arg
        }
    }

    descriptor.extensionReceiverParameter?.let {
        val arg = args[it]
        if (arg != null) {
            this.extensionReceiver = arg
        }
    }

    descriptor.valueParameters.forEach {
        val arg = args[it]
        if (arg != null) {
            this.putValueArgument(it.index, arg)
        }
    }
}

internal fun IrMemberAccessExpression.addArguments(args: List<Pair<ParameterDescriptor, IrExpression>>) =
        this.addArguments(args.toMap())

internal fun IrExpression.isNullConst() = this is IrConst<*> && this.kind == IrConstKind.Null

fun IrCall.usesDefaultArguments(): Boolean =
        this.descriptor.valueParameters.any { this.getValueArgument(it) == null }

fun IrElement.getCompilerMessageLocation(containingFile: IrFile): CompilerMessageLocation? {
    val sourceRangeInfo = containingFile.fileEntry.getSourceRangeInfo(this.startOffset, this.endOffset)
    return CompilerMessageLocation.create(
            path = sourceRangeInfo.filePath,
            line = sourceRangeInfo.startLineNumber,
            column = sourceRangeInfo.startColumnNumber,
            lineContent = null // TODO: retrieve the line content.
    )
}

fun IrFunction.createParameterDeclarations() {
    fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            innerStartOffset(this), innerEndOffset(this),
            IrDeclarationOrigin.DEFINED,
            this
    )

    dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
    extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

    assert(valueParameters.isEmpty())
    descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

    assert(typeParameters.isEmpty())
    descriptor.typeParameters.mapTo(typeParameters) {
        IrTypeParameterImpl(
                innerStartOffset(it), innerEndOffset(it),
                IrDeclarationOrigin.DEFINED,
                it
        )
    }
}

fun IrClass.createParameterDeclarations() {
    descriptor.thisAsReceiverParameter.let {
        thisReceiver = IrValueParameterImpl(
                innerStartOffset(it), innerEndOffset(it),
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                it
        )
    }

    assert(typeParameters.isEmpty())
    descriptor.declaredTypeParameters.mapTo(typeParameters) {
        IrTypeParameterImpl(
                innerStartOffset(it), innerEndOffset(it),
                IrDeclarationOrigin.DEFINED,
                it
        )
    }
}

private fun IrElement.innerStartOffset(descriptor: DeclarationDescriptorWithSource): Int =
        descriptor.startOffset ?: this.startOffset

private fun IrElement.innerEndOffset(descriptor: DeclarationDescriptorWithSource): Int =
       descriptor.endOffset ?: this.endOffset

val DeclarationDescriptorWithSource.startOffset: Int? get() = (this.source as? PsiSourceElement)?.psi?.startOffset
val DeclarationDescriptorWithSource.endOffset: Int? get() = (this.source as? PsiSourceElement)?.psi?.endOffset

val DeclarationDescriptorWithSource.startOffsetOrUndefined: Int get() = startOffset ?: UNDEFINED_OFFSET
val DeclarationDescriptorWithSource.endOffsetOrUndefined: Int get() = endOffset ?: UNDEFINED_OFFSET

val IrClassSymbol.functions: Sequence<IrSimpleFunctionSymbol>
    get() = this.owner.declarations.asSequence().filterIsInstance<IrSimpleFunction>().map { it.symbol }

val IrClassSymbol.constructors: Sequence<IrConstructorSymbol>
    get() = this.owner.declarations.asSequence().filterIsInstance<IrConstructor>().map { it.symbol }

val IrFunction.explicitParameters: List<IrValueParameterSymbol>
    get() = (listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters).map { it.symbol }

val IrValueParameter.type: KotlinType
    get() = this.descriptor.type

val IrClass.defaultType: KotlinType
    get() = this.descriptor.defaultType

class IrSymbolBindingChecker : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)

        expression.symbol.ensureBound(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        super.visitFunctionAccess(expression)

        expression.symbol.ensureBound(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        super.visitFunctionReference(expression)

        expression.symbol.ensureBound(expression)
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)

        expression.superQualifierSymbol?.ensureBound(expression)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        super.visitPropertyReference(expression)

        expression.field?.ensureBound(expression)
        expression.getter?.ensureBound(expression)
        expression.setter?.ensureBound(expression)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        super.visitLocalDelegatedPropertyReference(expression)

        expression.delegate.ensureBound(expression)
        expression.getter.ensureBound(expression)
        expression.setter?.ensureBound(expression)
    }

    override fun visitReturn(expression: IrReturn) {
        super.visitReturn(expression)

        expression.returnTargetSymbol.ensureBound(expression)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        super.visitInstanceInitializerCall(expression)

        expression.classSymbol.ensureBound(expression)
    }

    private fun IrSymbol.ensureBound(expression: IrExpression) {
        if (!this.isBound) {
            throw Error("Unbound symbol ${this} found in ${expression.render()}")
        }
    }

}
