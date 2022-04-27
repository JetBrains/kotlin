/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.UnlinkedDeclarationsSupport.UnlinkedMarkerTypeHandler
import org.jetbrains.kotlin.descriptors.Modality
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
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

internal class UnlinkedDeclarationsProcessor(
    private val builtIns: IrBuiltIns,
    private val unlinkedClassifiers: Set<IrClassifierSymbol>,
    private val unlinkedMarkerTypeHandler: UnlinkedMarkerTypeHandler,
    private val messageLogger: IrMessageLogger
) {

    companion object {
        private val errorOrigin = object : IrStatementOriginImpl("LINKAGE ERROR") {}
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

                klass.reportUnlinkedSymbolsWarning("Class", klass.fqNameForIrSerialization)

                anonInitializer.body.statements.add(klass.throwLinkageError(klass.symbol))

                klass.superTypes = klass.superTypes.filter { !it.isUnlinked() }
            }
        }
    }

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

    private fun IrDeclaration.reportUnlinkedSymbolsWarning(kind: String, fqn: FqName) {
        reportWarning("$kind declaration ${fqn.asString()} contains unlinked symbols", location())
    }

    private fun reportWarning(message: String, location: IrMessageLogger.Location?) {
        messageLogger.report(IrMessageLogger.Severity.WARNING, message, location)
    }

    fun signatureTransformer(): IrElementTransformerVoid = SignatureTransformer()

    private inner class SignatureTransformer : IrElementTransformerVoid() {
        private val implementedFakeOverrideProperties = hashSetOf<IrProperty>()

        private val IrFunction.isAccessorOfImplementedFakeOverrideProperty: Boolean
            get() = (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner in implementedFakeOverrideProperties

        override fun visitProperty(declaration: IrProperty): IrStatement {
            val newProperty = declaration.replaceIfUnimplementedFakeOverride()

            val isImplementedFakeOverride = newProperty != declaration
            if (isImplementedFakeOverride) implementedFakeOverrideProperties += newProperty

            newProperty.transformChildrenVoid()

            if (isImplementedFakeOverride) implementedFakeOverrideProperties -= newProperty

            return newProperty
        }

        override fun visitFunction(declaration: IrFunction): IrStatement {
            val newFunction = declaration.replaceIfUnimplementedFakeOverride()
            val removedUnlinkedTypes = newFunction.fixUnlinkedTypes()

            val isImplementedFakeOverride = newFunction != declaration || declaration.isAccessorOfImplementedFakeOverrideProperty

            return newFunction.transformBodyIfNecessary(isImplementedFakeOverride, removedUnlinkedTypes)
        }

        override fun visitField(declaration: IrField): IrStatement {
            if (declaration.type.isUnlinked()) {
                val fqn = declaration.correspondingPropertySymbol?.owner?.fqNameWhenAvailable ?: declaration.fqNameForIrSerialization
                val kind = if (declaration.correspondingPropertySymbol != null) "Property" else "Field"
                declaration.reportUnlinkedSymbolsWarning(kind, fqn)

                declaration.type = unlinkedMarkerTypeHandler.unlinkedMarkerType
                declaration.initializer = null
            } else {
                declaration.transformChildrenVoid()
            }
            return declaration
        }

        /**
         * Replaces an [IrProperty] or [IrSimpleFunction] that is abstract fake override in non-abstract class
         * by the corresponding non-abstract IR element.
         */
        private fun <T : IrDeclaration> T.replaceIfUnimplementedFakeOverride(): T {
            if (this !is IrOverridableDeclaration<*> || !isFakeOverride || modality != Modality.ABSTRACT) return this

            val clazz = parentAsClass
            if (clazz.modality == Modality.ABSTRACT || clazz.modality == Modality.SEALED) return this

            return deepCopyWithImplementedFakeOverrides()
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
                val functionKind = when {
                    this@transformBodyIfNecessary is IrSimpleFunction && correspondingPropertySymbol != null -> "property accessor"
                    this@transformBodyIfNecessary is IrConstructor -> "constructor"
                    else -> "function"
                }

                val errorMessages = listOfNotNull(
                    if (isImplementedFakeOverride)
                        buildString {
                            append("Abstract ").append(functionKind).append(" ")
                            append(fqNameForIrSerialization.asString())
                            append(" is not implemented in non-abstract class ")
                            append(parent.fqNameForIrSerialization.asString())
                        }
                    else null,
                    if (removedUnlinkedTypes.isNotEmpty())
                        buildString {
                            append("The signature of ").append(functionKind).append(" ")
                            append(fqNameForIrSerialization.asString())
                            append(" contains unlinked symbols: ")
                            removedUnlinkedTypes.joinTo(this) { it.render() }
                        }
                    else null
                )

                errorMessages.forEach { reportWarning(it, location()) }

                body?.let { body ->
                    val bb = body as IrBlockBody
                    bb.statements.clear()
                    bb.statements.add(throwLinkageError(unlinkedSymbol = null, errorMessages.joinToString(separator = "; ")))
                }
            }

            return this
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

    private fun IrElement.throwLinkageError(unlinkedSymbol: IrSymbol?, message: String = "Unlinked IR symbol"): IrCall {
        val messageLiteral = message + unlinkedSymbol?.signature?.render()?.let { " $it" }.orEmpty()

        val irCall = IrCallImpl(startOffset, endOffset, builtIns.nothingType, builtIns.linkageErrorSymbol, 0, 1, errorOrigin)
        irCall.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, builtIns.stringType, messageLiteral))
        return irCall
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
            composite.statements.add(expression.throwLinkageError(expression.typeOperandClassifier))

            return composite
        }

        override fun visitExpression(expression: IrExpression): IrExpression {
            if (expression.type.isUnlinked() || expression.type.isUnlinkedMarkerType()) {
                reportWarning("Expression type contains unlinked symbol", currentFile?.location(expression.startOffset))
                return expression.throwLinkageError(unlinkedSymbol = null, "Unlinked type of IR expression")
            }
            return super.visitExpression(expression)
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
            expression.transformChildrenVoid()
            val symbol = expression.symbol

            if (!symbol.isUnlinked() && !expression.type.isUnlinked()) return expression

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

            composite.statements.add(
                if (!symbol.isBound)
                    expression.throwLinkageError(symbol)
                else
                    expression.throwLinkageError(symbol, "Unlinked type in signature of IR symbol")
            )

            return composite
        }

        override fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression {
            expression.transformChildrenVoid()
            val symbol = expression.symbol
            if (!symbol.isUnlinked()) {
                return expression
            }

            reportWarning(
                "Accessing field contains unlinked symbol ${symbol.signature?.render()}",
                currentFile?.location(expression.startOffset)
            )

            val composite = IrCompositeImpl(expression.startOffset, expression.endOffset, builtIns.nothingType, expression.origin)
            expression.receiver?.let { composite.statements.add(it) }
            if (expression is IrSetField) {
                composite.statements.add(expression.value)
            }
            composite.statements.add(
                if (!symbol.isBound)
                    expression.throwLinkageError(symbol)
                else
                    expression.throwLinkageError(symbol, "Field type is unlinked in IR symbol")
            )
            return composite
        }

        override fun visitClassReference(expression: IrClassReference): IrExpression {
            if (expression.symbol.isUnlinked()) {
                reportWarning(
                    "Accessing class contains unlinked symbol ${expression.symbol.signature?.render()}",
                    currentFile?.location(expression.startOffset)
                )
                return expression.throwLinkageError(expression.symbol)
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