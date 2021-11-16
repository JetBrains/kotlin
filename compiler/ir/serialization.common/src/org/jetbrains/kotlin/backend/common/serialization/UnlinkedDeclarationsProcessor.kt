/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.types.Variance

internal class UnlinkedDeclarationsProcessor(
    private val builtIns: IrBuiltIns,
    private val unlinkedClassifiers: Set<IrClassifierSymbol>,
    private val messageLogger: IrMessageLogger
) {

    companion object {
        val errorOrigin = object : IrStatementOriginImpl("LINKAGE ERROR") {}
    }

    fun addLinkageErrorIntoUnlinkedClasses() {
        for (u in unlinkedClassifiers) {
            if (u is IrClassSymbol) {
                val klass = u.owner
                val anonInitializer = klass.declarations.firstOrNull { it is IrAnonymousInitializer } as IrAnonymousInitializer? ?: run {
                    builtIns.irFactory.createAnonymousInitializer(
                        klass.startOffset,
                        klass.endOffset,
                        IrDeclarationOrigin.DEFINED,
                        IrAnonymousInitializerSymbolImpl()
                    ).also {
                        it.body = builtIns.irFactory.createBlockBody(klass.startOffset, klass.endOffset)
                        it.parent = klass
                        klass.declarations.add(it)
                    }
                }
                anonInitializer.body.statements.clear()

                klass.reportWarning("Class", klass.fqNameForIrSerialization.asString())

                anonInitializer.body.statements.add(klass.throwLinkageError(klass.symbol.signature?.render()))

                klass.superTypes = klass.superTypes.filter { !it.isUnlinked() }
            }
        }
    }

    private val errorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)

    private fun IrDeclaration.location(): IrMessageLogger.Location? = fileOrNull?.location(startOffset)

    private fun IrFile.location(offset: Int): IrMessageLogger.Location {
        val module = module.name
        val fileEntry = fileEntry
        val fileName = fileEntry.name
        val lineNumber = fileEntry.getLineNumber(offset) + 1 // since humans count from 1, not 0
        val columnNumber = fileEntry.getColumnNumber(offset) + 1
        // unsure whether should module name be added here
        return IrMessageLogger.Location("$module @ $fileName", lineNumber, columnNumber)
    }

    private fun IrDeclaration.reportWarning(kind: String, fqn: String) {
        reportWarning("$kind declaration $fqn contains unlinked symbols", location())
    }

    private fun reportWarning(message: String, location: IrMessageLogger.Location?) {
        messageLogger.report(IrMessageLogger.Severity.WARNING, message, location)
    }

    fun signatureTransformer(): IrElementTransformerVoid = SignatureTransformer()

    private inner class SignatureTransformer : IrElementTransformerVoid() {

        override fun visitFunction(declaration: IrFunction): IrStatement {
            var linked = true
            fun IrValueParameter.fixType() {
                if (type.isUnlinked()) {
                    linked = false
                    type = errorType
                    defaultValue = null
                }
                varargElementType?.let {
                    if (it.isUnlinked()) {
                        varargElementType = errorType
                    }
                }
            }
            declaration.dispatchReceiverParameter?.fixType()
            declaration.extensionReceiverParameter?.fixType()
            declaration.valueParameters.forEach { it.fixType() }
            if (declaration.returnType.isUnlinked()) {
                linked = false
                declaration.returnType = errorType
            }
            declaration.typeParameters.forEach {
                if (it.superTypes.any { s -> s.isUnlinked() }) {
                    linked = false
                    it.superTypes = listOf(errorType)
                }
            }
            if (linked) {
                declaration.transformChildrenVoid()
            } else {
                declaration.reportWarning("Function", declaration.fqNameForIrSerialization.asString())

                declaration.body?.let { body ->
                    val bb = (body as IrBlockBody)
                    bb.statements.clear()
                    bb.statements.add(declaration.throwLinkageError("Unlinked type in function signature"))
                }
            }
            return declaration
        }

        override fun visitField(declaration: IrField): IrStatement {
            if (declaration.type.isUnlinked()) {

                val fqn = declaration.correspondingPropertySymbol?.owner?.fqNameWhenAvailable ?: declaration.fqNameForIrSerialization
                val kind = if (declaration.correspondingPropertySymbol != null) "Property" else "Field"
                declaration.reportWarning(kind, fqn.asString())

                declaration.type = errorType
                declaration.initializer?.let {
                    it.expression = it.expression.throwLinkageError("Unlinked type of property")
                }
            } else {
                declaration.transformChildrenVoid()
            }
            return declaration
        }
    }

    private fun IrSymbol.isUnlinked(): Boolean {
        if (!isBound) return true
        when (this) {
            is IrClassifierSymbol -> this.isUnlinked()
            is IrPropertySymbol -> {
                owner.getter?.let { if (it.symbol.isUnlinked()) return true }
                owner.setter?.let { if (it.symbol.isUnlinked()) return true }
                owner.backingField?.let { return it.symbol.isUnlinked() }
            }
            is IrFunctionSymbol -> return isUnlinked()
        }
        return false
    }

    private fun IrClassifierSymbol.isUnlinked(): Boolean = !isBound || this in unlinkedClassifiers

    private fun IrType.isUnlinked(): Boolean {
        val simpleType = this as? IrSimpleType ?: return false

        if (simpleType.classifier.isUnlinked()) return true

        return simpleType.arguments.any { it is IrTypeProjection && it.type.isUnlinked() }
    }

    private fun IrFieldSymbol.isUnlinked(): Boolean {
        return owner.type is IrErrorType
    }

    private fun IrFunctionSymbol.isUnlinked(): Boolean {
        val function = owner
        if (function.returnType is IrErrorType) return true
        if (function.dispatchReceiverParameter?.type is IrErrorType) return true
        if (function.extensionReceiverParameter?.type is IrErrorType) return true
        if (function.valueParameters.any { it.type is IrErrorType }) return true
        if (function.typeParameters.any { tp -> tp.superTypes.any { st -> st is IrErrorType } }) return true
        return false
    }

    private fun IrElement.throwLinkageError(message: String?): IrCall {
        return IrCallImpl(startOffset, endOffset, builtIns.nothingType, builtIns.linkageErrorSymbol, 0, 1, errorOrigin).also { call ->
            val messageLiteral = "Linkage error of symbol: ${message ?: ""}"
            call.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, builtIns.stringType, messageLiteral))
        }
    }

    fun usageTransformer(): IrElementTransformerVoid = UsageTransformer()

    private inner class UsageTransformer : IrElementTransformerVoid() {

        private var currentFile: IrFile? = null

        override fun visitFile(declaration: IrFile): IrFile {
            currentFile = declaration
            return super.visitFile(declaration).also { currentFile = null }
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
            expression.transformChildrenVoid()

            val checkType = expression.typeOperandClassifier

            if (!checkType.isUnlinked()) return expression

            reportWarning(
                "TypeOperator contains unlinked symbol ${checkType.signature?.render() ?: ""}",
                currentFile?.location(expression.startOffset)
            )

            val composite = IrCompositeImpl(expression.startOffset, expression.endOffset, builtIns.nothingType)

            composite.statements.add(expression.argument)
            composite.statements.add(expression.throwLinkageError(expression.typeOperandClassifier.signature?.render()))

            return composite
        }

        override fun visitExpression(expression: IrExpression): IrExpression {
            if (expression.type.isUnlinked() || expression.type is IrErrorType) {
                reportWarning("Expression type contains unlinked symbol", currentFile?.location(expression.startOffset))
                return expression.throwLinkageError("Unlinked type of expression")
            }
            return super.visitExpression(expression)
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
            expression.transformChildrenVoid()
            val symbol = expression.symbol

            if (!symbol.isUnlinked()) return expression

            reportWarning(
                "Accessing declaration contains unlinked symbol ${symbol.signature?.render() ?: ""}",
                currentFile?.location(expression.startOffset)
            )

            val composite = IrCompositeImpl(expression.startOffset, expression.endOffset, builtIns.nothingType, expression.origin)

            expression.dispatchReceiver?.let { composite.statements.add(it) }
            expression.extensionReceiver?.let { composite.statements.add(it) }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                arg?.let { composite.statements.add(it) }
            }

            val message =
                if (!symbol.isBound) "Unlinked symbol: ${symbol.signature?.render()}" else "Unlinked type in signature of ${symbol.signature?.render()}"
            composite.statements.add(expression.throwLinkageError(message))

            return composite
        }

        override fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression {
            expression.transformChildrenVoid()
            val symbol = expression.symbol
            if (!symbol.isUnlinked()) {
                return expression
            }

            reportWarning(
                "Accessing field contains unlinked symbol ${symbol.signature?.render() ?: ""}",
                currentFile?.location(expression.startOffset)
            )

            val composite = IrCompositeImpl(expression.startOffset, expression.endOffset, builtIns.nothingType, expression.origin)
            expression.receiver?.let { composite.statements.add(it) }
            if (expression is IrSetField) {
                composite.statements.add(expression.value)
            }
            val message = if (!symbol.isBound) "Unlinked symbol: ${symbol.signature?.render()}" else "Field type is unlinked"
            composite.statements.add(expression.throwLinkageError(message))
            return composite
        }

        override fun visitClassReference(expression: IrClassReference): IrExpression {
            if (expression.symbol.isUnlinked()) {
                val signRender = expression.symbol.signature?.render()
                reportWarning(
                    "Accessing class contains unlinked symbol ${signRender ?: ""}",
                    currentFile?.location(expression.startOffset)
                )
                return expression.throwLinkageError(signRender)
            }
            return expression
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.transformChildrenVoid()

            fun <S : IrSymbol> IrOverridableDeclaration<S>.filterOverriddens() {
                overriddenSymbols = overriddenSymbols.filter { it.isBound }
            }

            for (member in declaration.declarations) {
                when (member) {
                    is IrSimpleFunction -> member.filterOverriddens()
                    is IrProperty -> {
                        member.filterOverriddens()
                        member.getter?.filterOverriddens()
                        member.setter?.filterOverriddens()
                    }
                }
            }

            return declaration
        }
    }
}