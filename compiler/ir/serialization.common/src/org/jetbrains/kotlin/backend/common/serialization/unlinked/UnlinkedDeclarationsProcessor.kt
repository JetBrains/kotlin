/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.UnlinkedDeclarationsProcessor.Companion.MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UnlinkedIrElementRenderer.appendDeclaration
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UnlinkedIrElementRenderer.renderError
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UsedClassifierSymbolStatus.Companion.isUnlinked
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.IrMessageLogger.Location
import org.jetbrains.kotlin.ir.util.IrMessageLogger.Severity
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.utils.addIfNotNull

internal class UnlinkedDeclarationsProcessor(
    private val builtIns: IrBuiltIns,
    private val usedClassifierSymbols: UsedClassifierSymbols,
    private val unlinkedMarkerTypeHandler: UnlinkedMarkerTypeHandler,
    private val messageLogger: IrMessageLogger
) {
    fun addLinkageErrorIntoUnlinkedClasses() {
        usedClassifierSymbols.forEachClassSymbolToPatch { unlinkedSymbol ->
            val clazz = unlinkedSymbol.owner

            val anonInitializer = clazz.declarations.firstNotNullOfOrNull { it as? IrAnonymousInitializer }
                ?: builtIns.irFactory.createAnonymousInitializer(
                    clazz.startOffset,
                    clazz.endOffset,
                    IrDeclarationOrigin.DEFINED,
                    IrAnonymousInitializerSymbolImpl()
                ).also {
                    it.body = builtIns.irFactory.createBlockBody(clazz.startOffset, clazz.endOffset)
                    it.parent = clazz
                    clazz.declarations.add(it)
                }
            anonInitializer.body.statements.clear()
            anonInitializer.body.statements += clazz.throwLinkageError() // TODO: which exactly classifiers are unlinked?

            clazz.superTypes = clazz.superTypes.filter { !it.isUnlinked() }
        }
    }

    fun signatureTransformer(): IrElementTransformerVoid = SignatureTransformer()

    private inner class SignatureTransformer : IrElementTransformerVoid() {
        override fun visitFunction(declaration: IrFunction): IrStatement {
            val removedUnlinkedTypes = declaration.fixUnlinkedTypes()
            val isImplementedFakeOverride = declaration.origin == MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION

            return declaration.transformBodyIfNecessary(isImplementedFakeOverride, removedUnlinkedTypes)
        }

        override fun visitField(declaration: IrField): IrStatement {
            if (declaration.type.isUnlinked()) {
                // TODO: it would be more precise to use the set of unlinked symbols than a collection of unlinked types here.
                declaration.logLinkageError(listOfNotNull((declaration.type as? IrSimpleType)?.classifier))
                declaration.type = unlinkedMarkerTypeHandler.unlinkedMarkerType
                declaration.initializer = null
            } else {
                declaration.transformChildrenVoid()
            }
            return declaration
        }

        override fun visitVariable(declaration: IrVariable): IrStatement {
            if (declaration.type.isUnlinked()) {
                // TODO: it would be more precise to use the set of unlinked symbols than a collection of unlinked types here.
                declaration.logLinkageError(listOfNotNull((declaration.type as? IrSimpleType)?.classifier))
                declaration.type = unlinkedMarkerTypeHandler.unlinkedMarkerType
                declaration.initializer = null
            } else {
                declaration.transformChildrenVoid()
            }
            return declaration
        }

        /**
         * Returns the set of all unlinked types encountered during transformation of the given [IrFunction].
         * Or empty set if there were no unlinked types.
         */
        private fun IrFunction.fixUnlinkedTypes(): Set<IrType> = buildSet {
            fun IrValueParameter.fixType() {
                if (type.isUnlinked()) {
                    this@buildSet += type
                    type = unlinkedMarkerTypeHandler.unlinkedMarkerType
                    defaultValue = null
                }
                varargElementType?.let {
                    if (it.isUnlinked()) {
                        this@buildSet += it
                        varargElementType = unlinkedMarkerTypeHandler.unlinkedMarkerType
                    }
                }
            }

            dispatchReceiverParameter?.fixType()
            extensionReceiverParameter?.fixType()
            valueParameters.forEach { it.fixType() }
            if (returnType.isUnlinked()) {
                this += returnType
                returnType = unlinkedMarkerTypeHandler.unlinkedMarkerType
            }
            typeParameters.forEach {
                val unlinkedSuperType = it.superTypes.firstOrNull { s -> s.isUnlinked() }
                if (unlinkedSuperType != null) {
                    this += unlinkedSuperType
                    it.superTypes = listOf(unlinkedMarkerTypeHandler.unlinkedMarkerType)
                }
            }
        }

        private fun IrFunction.transformBodyIfNecessary(
            isImplementedFakeOverride: Boolean,
            removedUnlinkedTypes: Set<IrType>
        ): IrFunction {
            if (!isImplementedFakeOverride && removedUnlinkedTypes.isEmpty()) {
                transformChildrenVoid()
            } else {
                val errorMessages = listOfNotNull(
                    if (isImplementedFakeOverride)
                        buildString {
                            append("Abstract ").appendDeclaration(this@transformBodyIfNecessary)
                            append(" is not implemented in non-abstract ").appendDeclaration(parentAsClass)
                        }
                    else null,
                    if (removedUnlinkedTypes.isNotEmpty()) {
                        // TODO: it would be more precise to use the set of unlinked symbols than a collection of unlinked types here.
                        composeUnlinkedSymbolsErrorMessage(removedUnlinkedTypes.mapNotNull { (it as? IrSimpleType)?.classifier })
                    } else null
                )

                body?.let { body ->
                    val bb = body as IrBlockBody
                    bb.statements.clear()
                    bb.statements += throwLinkageError(errorMessages, location())
                }
            }

            return this
        }
    }

    private fun IrSymbol.isUnlinked(): Boolean {
        if (!isBound) return true
        when (this) {
            is IrClassifierSymbol -> isUnlinked()
            is IrPropertySymbol -> {
                owner.getter?.let { if (it.symbol.isUnlinked()) return true }
                owner.setter?.let { if (it.symbol.isUnlinked()) return true }
                owner.backingField?.let { return it.symbol.isUnlinked() }
            }
            is IrFunctionSymbol -> return isUnlinked()
        }
        return false
    }

    private fun IrClassifierSymbol.isUnlinked(): Boolean = !isBound || usedClassifierSymbols[this].isUnlinked

    private fun IrType.isUnlinked(): Boolean {
        val simpleType = this as? IrSimpleType ?: return false

        if (simpleType.classifier.isUnlinked()) return true

        return simpleType.arguments.any { it is IrTypeProjection && it.type.isUnlinked() }
    }

    private fun IrFieldSymbol.isUnlinked(): Boolean {
        return owner.type.isUnlinkedMarkerType()
    }

    private fun IrFunctionSymbol.isUnlinked(): Boolean {
        val function = owner
        if (function.returnType.isUnlinkedMarkerType()) return true
        if (function.dispatchReceiverParameter?.type?.isUnlinkedMarkerType() == true) return true
        if (function.extensionReceiverParameter?.type?.isUnlinkedMarkerType() == true) return true
        if (function.valueParameters.any { it.type.isUnlinkedMarkerType() }) return true
        if (function.typeParameters.any { tp -> tp.superTypes.any { st -> st.isUnlinkedMarkerType() } }) return true
        return false
    }

    // That's not the same as IrType.isUnlinked()!
    private fun IrType.isUnlinkedMarkerType(): Boolean {
        return with(unlinkedMarkerTypeHandler) { isUnlinkedMarkerType() }
    }

    private fun IrElement.composeUnlinkedSymbolsErrorMessage(unlinkedSymbols: Collection<IrSymbol>) =
        renderError(this@composeUnlinkedSymbolsErrorMessage, unlinkedSymbols)

    private fun IrDeclaration.throwLinkageError(unlinkedSymbols: Collection<IrSymbol> = emptyList()): IrCall =
        throwLinkageError(
            messages = listOf(composeUnlinkedSymbolsErrorMessage(unlinkedSymbols)),
            location = location()
        )

    private fun IrElement.throwLinkageError(messages: List<String>, location: Location?): IrCall {
        check(messages.isNotEmpty())

        messages.forEach { logLinkageError(it, location) }

        val irCall = IrCallImpl(startOffset, endOffset, builtIns.nothingType, builtIns.linkageErrorSymbol, 0, 1, ERROR_ORIGIN)
        irCall.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, builtIns.stringType, messages.joinToString("\n")))
        return irCall
    }

    private fun IrDeclaration.logLinkageError(unlinkedSymbols: Collection<IrSymbol>) {
        logLinkageError(
            composeUnlinkedSymbolsErrorMessage(unlinkedSymbols),
            location()
        )
    }

    private fun logLinkageError(message: String, location: Location?) {
        messageLogger.report(Severity.WARNING, message, location) // It's OK. We log it as a warning.
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

            val classifierSymbol = expression.typeOperandClassifier
            return if (classifierSymbol.isUnlinked())
                IrCompositeImpl(expression.startOffset, expression.endOffset, builtIns.nothingType).apply {
                    statements += expression.argument
                    statements += expression.throwLinkageError(classifierSymbol)
                }
            else
                expression
        }

        override fun visitExpression(expression: IrExpression): IrExpression {
            return if (expression.type.isUnlinked() || expression.type.isUnlinkedMarkerType())
                expression.throwLinkageError() // TODO: which exactly classifiers are unlinked?
            else
                super.visitExpression(expression)
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
            expression.transformChildrenVoid()

            return if (!expression.symbol.isUnlinked() && !expression.type.isUnlinked())
                expression
            else
                IrCompositeImpl(expression.startOffset, expression.endOffset, builtIns.nothingType, expression.origin).apply {
                    statements.addIfNotNull(expression.dispatchReceiver)
                    statements.addIfNotNull(expression.extensionReceiver)

                    for (i in 0 until expression.valueArgumentsCount) {
                        statements.addIfNotNull(expression.getValueArgument(i))
                    }

                    statements += expression.throwLinkageError() // TODO: which exactly classifiers are unlinked?
                }
        }

        override fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression {
            expression.transformChildrenVoid()

            return if (!expression.symbol.isUnlinked())
                expression
            else
                IrCompositeImpl(expression.startOffset, expression.endOffset, builtIns.nothingType, expression.origin).apply {
                    statements.addIfNotNull(expression.receiver)
                    if (expression is IrSetField)
                        statements += expression.value
                    statements += expression.throwLinkageError(expression.symbol)
                }
        }

        override fun visitClassReference(expression: IrClassReference): IrExpression {
            return if (expression.symbol.isUnlinked())
                expression.throwLinkageError(expression.symbol)
            else
                expression
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.transformChildrenVoid()

            fun <S : IrSymbol> IrOverridableDeclaration<S>.filterOverriddenSymbols() {
                overriddenSymbols = overriddenSymbols.filter { symbol ->
                    symbol.isBound
                            // Handle the case when the overridden declaration became private.
                            && (symbol.owner as? IrDeclarationWithVisibility)?.visibility != DescriptorVisibilities.PRIVATE
                }
            }

            for (member in declaration.declarations) {
                when (member) {
                    is IrSimpleFunction -> member.filterOverriddenSymbols()
                    is IrProperty -> {
                        member.filterOverriddenSymbols()
                        member.getter?.filterOverriddenSymbols()
                        member.setter?.filterOverriddenSymbols()
                    }
                }
            }

            return declaration
        }

        private fun IrExpression.throwLinkageError(unlinkedSymbol: IrSymbol? = null): IrCall =
            throwLinkageError(
                messages = listOf(composeUnlinkedSymbolsErrorMessage(listOfNotNull(unlinkedSymbol))),
                location = locationIn(currentFile)
            )
    }

    companion object {
        private val ERROR_ORIGIN = object : IrStatementOriginImpl("LINKAGE ERROR") {}

        val MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION =
            object : IrDeclarationOriginImpl("MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION", isSynthetic = true) {}
    }
}

private fun IrDeclaration.location(): Location? = locationIn(fileOrNull)

private fun IrElement.locationIn(currentFile: IrFile?): Location? {
    if (currentFile == null) return null

    val moduleName: String = currentFile.module.name.asString()
    val filePath: String = currentFile.fileEntry.name

    val lineNumber: Int
    val columnNumber: Int

    when (val effectiveStartOffset = startOffsetOfFirstNonSyntheticIrElement()) {
        UNDEFINED_OFFSET -> {
            lineNumber = UNDEFINED_LINE_NUMBER
            columnNumber = UNDEFINED_COLUMN_NUMBER
        }
        else -> {
            lineNumber = currentFile.fileEntry.getLineNumber(effectiveStartOffset) + 1 // since humans count from 1, not 0
            columnNumber = currentFile.fileEntry.getColumnNumber(effectiveStartOffset) + 1
        }
    }

    // TODO: should module name still be added here?
    return Location("$moduleName @ $filePath", lineNumber, columnNumber)
}

private tailrec fun IrElement.startOffsetOfFirstNonSyntheticIrElement(): Int = when (this) {
    is IrPackageFragment -> UNDEFINED_OFFSET
    !is IrDeclaration -> {
        // We don't generate synthetic IR expressions in the course of partial linkage.
        startOffset
    }
    else -> when (origin) {
        MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION -> {
            // There is no sense to take coordinates from the declaration that does not exist in the code.
            // Let's take the coordinates of the parent.
            parent.startOffsetOfFirstNonSyntheticIrElement()
        }
        else -> startOffset
    }
}
