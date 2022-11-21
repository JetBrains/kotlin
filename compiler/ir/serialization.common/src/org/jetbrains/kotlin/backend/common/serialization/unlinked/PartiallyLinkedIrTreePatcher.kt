/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.LinkedClassifierStatus.Partially
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageCase.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartiallyLinkedStatementOrigin.PARTIAL_LINKAGE_RUNTIME_ERROR
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.IrMessageLogger.Location
import org.jetbrains.kotlin.ir.util.IrMessageLogger.Severity
import org.jetbrains.kotlin.ir.visitors.*
import java.util.*
import kotlin.properties.Delegates

internal class PartiallyLinkedIrTreePatcher(
    private val builtIns: IrBuiltIns,
    private val classifierExplorer: LinkedClassifierExplorer,
    private val stubGenerator: MissingDeclarationStubGenerator,
    private val messageLogger: IrMessageLogger
) {
    fun patch(roots: Collection<IrElement>) {
        roots.forEach { it.transformVoid(DeclarationTransformer()) }
        roots.forEach { it.transformVoid(ExpressionTransformer()) }
    }

    private fun IrElement.transformVoid(transformer: IrElementTransformerVoid) {
        transform(transformer, null)
    }

    private sealed class DeclarationTransformerContext {
        private val scheduledForRemoval = HashSet<IrDeclaration>()

        fun scheduleForRemoval(declaration: IrDeclaration) {
            scheduledForRemoval += declaration
        }

        abstract fun performRemoval()

        protected fun performRemoval(declarations: MutableList<out IrStatement>, container: IrElement) {
            val expectedToRemove = scheduledForRemoval.size
            if (expectedToRemove == 0) return

            var removed = declarations.size
            declarations.removeAll(scheduledForRemoval)
            removed -= declarations.size

            assert(expectedToRemove == removed) {
                "Expected to remove $expectedToRemove declarations, but removed only $removed in $container"
            }
        }

        class DeclarationContainer(val declarationContainer: IrDeclarationContainer) : DeclarationTransformerContext() {
            override fun performRemoval() = performRemoval(declarationContainer.declarations, declarationContainer)
        }

        class StatementContainer(val statementContainer: IrStatementContainer) : DeclarationTransformerContext() {
            override fun performRemoval() = performRemoval(statementContainer.statements, statementContainer)
        }
    }

    // Declarations are transformed top-down.
    private inner class DeclarationTransformer : IrElementTransformerVoid() {
        private val stack = ArrayDeque<DeclarationTransformerContext>()

        private fun <T : IrElement> T.transformChildren(): T {
            transformChildrenVoid()
            return this
        }

        private fun <T : IrDeclarationContainer> T.transformChildrenWithRemoval(): T =
            transformChildrenWithRemoval(DeclarationTransformerContext.DeclarationContainer(this))

        private fun <T : IrStatementContainer> T.transformChildrenWithRemoval(): T =
            transformChildrenWithRemoval(DeclarationTransformerContext.StatementContainer(this))

        private fun <T : IrElement> T.transformChildrenWithRemoval(context: DeclarationTransformerContext): T {
            stack.push(context)
            transformChildrenVoid()
            assert(stack.pop() === context)

            context.performRemoval()

            return this
        }

        private fun IrDeclaration.scheduleForRemoval() {
            // The declarations with origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION are already effectively removed.
            if (origin != PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                stack.peek().scheduleForRemoval(this)
        }

        override fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment {
            return declaration.transformChildrenWithRemoval()
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            // Discover the reason why the class itself is partially linked.
            val partialLinkageReason = declaration.symbol.partialLinkageReason()
            if (partialLinkageReason != null) {
                // Transform the reason into the most appropriate linkage case.
                val partialLinkageCase = when (partialLinkageReason) {
                    is Partially.MissingClassifier -> MissingDeclaration(declaration.symbol)
                    is Partially.MissingEnclosingClass -> MissingEnclosingClass(declaration.symbol)
                    is Partially.DueToOtherClassifier -> DeclarationUsesPartiallyLinkedClassifier(
                        declaration.symbol,
                        partialLinkageReason.rootCause
                    )
                }

                // Get anonymous initializer.
                val anonInitializer = declaration.declarations.firstNotNullOfOrNull { it as? IrAnonymousInitializer }
                    ?: builtIns.irFactory.createAnonymousInitializer(
                        declaration.startOffset,
                        declaration.endOffset,
                        PartiallyLinkedDeclarationOrigin.AUXILIARY_GENERATED_DECLARATION,
                        IrAnonymousInitializerSymbolImpl()
                    ).apply {
                        body = builtIns.irFactory.createBlockBody(declaration.startOffset, declaration.endOffset)
                        parent = declaration
                        declaration.declarations += this
                    }

                // Clean initializer body. Don't process underlying statements.
                anonInitializer.body.statements.clear()

                // Generate IR call that throws linkage error. Report compiler warning.
                anonInitializer.body.statements += partialLinkageCase.throwLinkageError(anonInitializer)

                // Finish processing of the current class.
                declaration.typeParameters.forEach { tp ->
                    tp.superTypes.toPartiallyLinkedMarkerTypeOrNull()?.let { newSuperType ->
                        tp.superTypes = listOf(newSuperType)
                    }
                }

                declaration.superTypes = declaration.superTypes.filter { !it.hasPartialLinkageReason() }

                /**
                 * Remove the class in the following cases:
                 * - It is a local class (or anonymous object)
                 * - It is an inner class
                 * - It is a class without non-inner underlying classes
                 *
                 * The removal of local/inner class leads to removal of all underlying declarations including any classes declared
                 * under the removed class. In all cases that could be only inner classes that share state with their containing
                 * class and that become partially linked together with the containing class.
                 *
                 * The removal of class of any other type is not performed: Such class may have nested classes that does not share
                 * state with the containing class and not necessarily become partially linked together with the containing class.
                 */
                if (declaration.isLocal || declaration.isInner || declaration.declarations.none { (it as? IrClass)?.isInner == false })
                    declaration.scheduleForRemoval()
            }

            // Process underlying declarations. Collect declarations to remove.
            return declaration.transformChildrenWithRemoval()
        }

        override fun visitFunction(declaration: IrFunction): IrStatement {
            (declaration as? IrOverridableDeclaration<*>)?.filterOverriddenSymbols()

            // IMPORTANT: It's necessary to overwrite types. Please don't move the statement below.
            val signaturePartialLinkageReason = declaration.rewriteTypesInFunction()

            // Compute the linkage case.
            val partialLinkageCase = when (declaration.origin) {
                PartiallyLinkedDeclarationOrigin.UNIMPLEMENTED_ABSTRACT_CALLABLE_MEMBER -> UnimplementedAbstractCallable(declaration as IrOverridableDeclaration<*>)
                PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION -> MissingDeclaration(declaration.symbol)
                else -> signaturePartialLinkageReason?.let { DeclarationUsesPartiallyLinkedClassifier(declaration.symbol, it) }
            }

            if (partialLinkageCase != null) {
                val blockBody = declaration.body as? IrBlockBody
                    ?: builtIns.irFactory.createBlockBody(declaration.startOffset, declaration.endOffset).apply { declaration.body = this }

                // Clean initializer body. Don't process underlying statements.
                blockBody.statements.clear()

                // Generate IR call that throws linkage error. Report compiler warning.
                blockBody.statements += partialLinkageCase.throwLinkageError(declaration)

                // Don't remove inline functions, that may harm linkage in K/N with static caches.
                if (!declaration.isInline) {
                    if (declaration.isTopLevelDeclaration) {
                        // Optimization: Remove unlinked top-level functions.
                        declaration.scheduleForRemoval()
                    } else if (declaration is IrSimpleFunction) {
                        // Optimization: Remove unlinked top-level properties.
                        val property = declaration.correspondingPropertySymbol?.owner
                        if (property?.isTopLevelDeclaration == true)
                            property.scheduleForRemoval()
                    }
                }

                // Return the function. There is nothing to process below it.
                return declaration
            }

            // Process underlying declarations. Collect declarations to remove.
            return declaration.transformChildren()
        }

        /**
         * Returns the first [Partially] from the first encountered partially linked type.
         */
        private fun IrFunction.rewriteTypesInFunction(): Partially? {
            // Remember the first assignment. Ignore all subsequent.
            var result: Partially? by Delegates.vetoable(null) { _, oldValue, _ -> oldValue == null }

            fun IrValueParameter.fixType() {
                val newType = type.toPartiallyLinkedMarkerTypeOrNull() ?: return
                type = newType
                if (varargElementType != null) varargElementType = newType
                defaultValue = null

                result = newType.partialLinkageReason
            }

            extensionReceiverParameter?.fixType()
            valueParameters.forEach { it.fixType() }

            returnType.toPartiallyLinkedMarkerTypeOrNull()?.let { newReturnType ->
                returnType = newReturnType
                result = newReturnType.partialLinkageReason
            }

            typeParameters.forEach { tp ->
                tp.superTypes.toPartiallyLinkedMarkerTypeOrNull()?.let { newSuperType ->
                    tp.superTypes = listOf(newSuperType)
                    result = newSuperType.partialLinkageReason
                }
            }

            dispatchReceiverParameter?.fixType() // The dispatcher (aka this) is intentionally the last one.

            return result
        }

        override fun visitProperty(declaration: IrProperty): IrStatement {
            declaration.filterOverriddenSymbols()
            return declaration.transformChildren()
        }

        override fun visitField(declaration: IrField): IrStatement {
            return declaration.type.toPartiallyLinkedMarkerTypeOrNull()?.let { newType ->
                val property = declaration.correspondingPropertySymbol?.owner
                if (property?.isTopLevelDeclaration == true) {
                    // Optimization: Remove unlinked top-level properties.
                    property.scheduleForRemoval()
                }

                declaration.type = newType
                declaration.initializer = null
                declaration
            } ?: declaration.transformChildren()
        }

        override fun visitVariable(declaration: IrVariable): IrStatement {
            return declaration.type.toPartiallyLinkedMarkerTypeOrNull()?.let { newType ->
                declaration.type = newType
                declaration.initializer = null
                declaration
            } ?: declaration.transformChildren()
        }

        override fun visitBlockBody(body: IrBlockBody): IrBody {
            return body.transformChildrenWithRemoval()
        }

        override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
            return expression.transformChildrenWithRemoval()
        }

        private fun <S : IrSymbol> IrOverridableDeclaration<S>.filterOverriddenSymbols() {
            overriddenSymbols = overriddenSymbols.filter { symbol ->
                val owner = symbol.owner as IrDeclaration
                owner.origin != PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION
                        // Handle the case when the overridden declaration became private.
                        && (owner as? IrDeclarationWithVisibility)?.visibility != DescriptorVisibilities.PRIVATE
            }
        }
    }

    private inner class ExpressionTransformer : IrElementTransformerVoid() {
        private var currentFile: IrFile? = null

        override fun visitFile(declaration: IrFile): IrFile {
            currentFile = declaration
            return try {
                super.visitFile(declaration)
            } finally {
                currentFile = null
            }
        }

        override fun visitBlockBody(body: IrBlockBody): IrBody {
            super.visitBlockBody(body)
            body.statements.eliminateDeadCodeStatements()
            return body
        }

        override fun visitReturn(expression: IrReturn) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(returnTargetSymbol)
        }

        override fun visitBlock(expression: IrBlock) = expression.maybeThrowLinkageError {
            if (this is IrReturnableBlock)
                checkReferencedDeclaration(symbol) ?: checkReferencedDeclaration(inlineFunctionSymbol)
            else null
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall) = expression.maybeThrowLinkageError {
            checkExpressionType(typeOperand)
        }

        override fun visitDeclarationReference(expression: IrDeclarationReference) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
        }

        override fun visitClassReference(expression: IrClassReference) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol) ?: checkExpressionType(classType)
        }

        override fun visitGetObjectValue(expression: IrGetObjectValue) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkReferencedDeclarationType(symbol.owner, "object") { it.kind == ClassKind.OBJECT }
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol) ?: checkExpressionTypeArguments()
        }

        override fun visitCall(expression: IrCall) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkReferencedDeclaration(superQualifierSymbol)
                ?: checkExpressionTypeArguments()
        }

        override fun visitConstructorCall(expression: IrConstructorCall) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkExpressionTypeArguments()
                ?: checkReferencedDeclarationType(expression.symbol.owner.parentAsClass, "regular class") { constructedClass ->
                    constructedClass.kind == ClassKind.CLASS || constructedClass.kind == ClassKind.ANNOTATION_CLASS
                }
        }

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkExpressionTypeArguments()
                ?: checkReferencedDeclarationType(expression.symbol.owner.parentAsClass, "enum class") { constructedClass ->
                    constructedClass.kind == ClassKind.ENUM_CLASS || constructedClass.symbol == builtIns.enumClass
                }
        }

        override fun visitFunctionReference(expression: IrFunctionReference) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkReferencedDeclaration(reflectionTarget)
                ?: checkExpressionTypeArguments()
        }

        override fun visitPropertyReference(expression: IrPropertyReference) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkReferencedDeclaration(getter)
                ?: checkReferencedDeclaration(setter)
                ?: checkReferencedDeclaration(field)
                ?: checkExpressionTypeArguments()
        }

        override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(classSymbol)
        }

        override fun visitExpression(expression: IrExpression) = expression.maybeThrowLinkageError { null }

        private inline fun <T : IrExpression> T.maybeThrowLinkageError(computePartialLinkageCase: T.() -> PartialLinkageCase?): IrExpression {
            // The codegen uses postorder traversal: Children are evaluated/executed before the containing expression.
            // So it's important to patch children and insert the necessary `throw IrLinkageError(...)` calls if necessary
            // before patching the containing expression itself. This would guarantee that the order of linkage errors in a program
            // would conform to the natural program execution flow.
            transformChildrenVoid()

            val partialLinkageCase = computePartialLinkageCase()
                ?: checkExpressionType(type) // Check something that is always present in every expression.
                ?: return apply { if (this is IrContainerExpression) statements.eliminateDeadCodeStatements() }

            // Collect direct children if `this` isn't an expression with branches.
            val directChildren = if (!hasBranches())
                DirectChildrenStatementsCollector().also(::acceptChildrenVoid).getResult() else null

            val linkageError = partialLinkageCase.throwLinkageError(element = this, file = currentFile)

            return if (directChildren?.statements?.isNotEmpty() == true)
                IrCompositeImpl(startOffset, endOffset, builtIns.nothingType, PARTIAL_LINKAGE_RUNTIME_ERROR).apply {
                    statements += directChildren.statements
                    if (!directChildren.hasPartialLinkageRuntimeError) statements += linkageError
                }
            else
                linkageError
        }

        private fun <T : IrExpression> T.checkExpressionType(type: IrType): PartialLinkageCase? {
            val partialLinkageReason = type.partialLinkageReason() ?: return null
            return ExpressionUsesPartiallyLinkedClassifier(this, partialLinkageReason)
        }

        private fun IrMemberAccessExpression<*>.checkExpressionTypeArguments(): PartialLinkageCase? {
            val partialLinkageReason = (0 until typeArgumentsCount).firstNotNullOfOrNull { index ->
                getTypeArgument(index)?.partialLinkageReason()
            } ?: return null
            return ExpressionUsesPartiallyLinkedClassifier(this, partialLinkageReason)
        }

        private fun <T : IrExpression> T.checkReferencedDeclaration(symbol: IrSymbol?): PartialLinkageCase? {
            symbol ?: return null

            if (!symbol.isBound && !symbol.isPublicApi) {
                // Such symbols might not be in SymbolTable. Just bind them in place.
                stubGenerator.getDeclaration(symbol)
            }

            val origin = (symbol.owner as? IrDeclaration)?.origin ?: return null

            if (origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                return ExpressionUsesMissingDeclaration(this, symbol)

            return when (symbol) {
                is IrClassifierSymbol -> ExpressionUsesPartiallyLinkedClassifier(this, symbol.partialLinkageReason() ?: return null)

                is IrEnumEntrySymbol -> checkReferencedDeclaration(symbol.owner.correspondingClass?.symbol)

                is IrPropertySymbol -> checkReferencedDeclaration(symbol.owner.getter?.symbol)
                    ?: checkReferencedDeclaration(symbol.owner.setter?.symbol)
                    ?: checkReferencedDeclaration(symbol.owner.backingField?.symbol)

                else -> {
                    val partialLinkageReasonInReferencedDeclaration = when (symbol) {
                        is IrFunctionSymbol -> with(symbol.owner) {
                            extensionReceiverParameter?.type?.precalculatedPartialLinkageReason()
                                ?: valueParameters.firstNotNullOfOrNull { it.type.precalculatedPartialLinkageReason() }
                                ?: returnType.precalculatedPartialLinkageReason()
                                ?: typeParameters.firstNotNullOfOrNull { it.superTypes.precalculatedPartialLinkageReason() }
                                ?: dispatchReceiverParameter?.type?.precalculatedPartialLinkageReason()
                        }

                        is IrFieldSymbol -> symbol.owner.type.precalculatedPartialLinkageReason()
                        is IrValueSymbol -> symbol.owner.type.precalculatedPartialLinkageReason()

                        else -> null
                    } ?: return null

                    ExpressionUsesDeclarationThatUsesPartiallyLinkedClassifier(this, symbol, partialLinkageReasonInReferencedDeclaration)
                }
            }
        }

        private fun <T : IrExpression, D : IrDeclaration> T.checkReferencedDeclarationType(
            declaration: D,
            expectedDeclarationDescription: String,
            checkDeclarationType: (D) -> Boolean
        ): PartialLinkageCase? {
            return if (!checkDeclarationType(declaration))
                ExpressionUsesWrongTypeOfDeclaration(this, declaration.symbol, expectedDeclarationDescription)
            else null
        }

        private fun IrType.precalculatedPartialLinkageReason(): Partially? =
            (this as? PartiallyLinkedMarkerType)?.partialLinkageReason ?: partialLinkageReason()

        private fun List<IrType>.precalculatedPartialLinkageReason(): Partially? =
            firstNotNullOfOrNull { it.precalculatedPartialLinkageReason() }

        /**
         * Removes statements after the first IR p.l. error (everything after the IR p.l. error if effectively dead code and do not need
         * to be kept in the IR tree).
         */
        private fun MutableList<IrStatement>.eliminateDeadCodeStatements() {
            var hasPartialLinkageRuntimeError = false
            removeIf {
                val needToRemove = hasPartialLinkageRuntimeError
                hasPartialLinkageRuntimeError = hasPartialLinkageRuntimeError || it.isPartialLinkageRuntimeError()
                needToRemove
            }
        }
    }

    private fun IrClassifierSymbol.partialLinkageReason(): Partially? = classifierExplorer.exploreSymbol(this)

    private fun IrType.partialLinkageReason(): Partially? = classifierExplorer.exploreType(this)
    private fun IrType.hasPartialLinkageReason(): Boolean = partialLinkageReason() != null

    private fun IrType.toPartiallyLinkedMarkerTypeOrNull(): PartiallyLinkedMarkerType? =
        partialLinkageReason()?.let { PartiallyLinkedMarkerType(builtIns, it) }

    private fun List<IrType>.toPartiallyLinkedMarkerTypeOrNull(): PartiallyLinkedMarkerType? =
        firstNotNullOfOrNull { it.toPartiallyLinkedMarkerTypeOrNull() }

    private fun PartialLinkageCase.throwLinkageError(declaration: IrDeclaration): IrCall =
        throwLinkageError(declaration, declaration.fileOrNull)

    private fun PartialLinkageCase.throwLinkageError(element: IrElement, file: IrFile?): IrCall {
        val errorMessage = renderErrorMessage()
        val locationInSourceCode = element.computeLocationInSourceCode(file)

        messageLogger.report(Severity.WARNING, errorMessage, locationInSourceCode) // It's OK. We log it as a warning.

        return IrCallImpl(
            startOffset = element.startOffset,
            endOffset = element.endOffset,
            type = builtIns.nothingType,
            symbol = builtIns.linkageErrorSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 1,
            origin = PARTIAL_LINKAGE_RUNTIME_ERROR
        ).apply {
            putValueArgument(0, IrConstImpl.string(startOffset, endOffset, builtIns.stringType, errorMessage))
        }
    }

    /**
     * Collects direct children statements up to the first IR p.l. error (everything after the IR p.l. error
     * if effectively dead code and do not need to be kept in the IR tree).
     */
    private class DirectChildrenStatementsCollector : IrElementVisitorVoid {
        data class DirectChildren(val statements: List<IrStatement>, val hasPartialLinkageRuntimeError: Boolean)

        private val children = mutableListOf<IrStatement>()
        private var hasPartialLinkageRuntimeError = false

        fun getResult() = DirectChildren(children, hasPartialLinkageRuntimeError)

        override fun visitElement(element: IrElement) {
            if (hasPartialLinkageRuntimeError) return
            val statement = element as? IrStatement ?: error("Not a statement: $element")
            children += statement
            hasPartialLinkageRuntimeError = statement.isPartialLinkageRuntimeError()
        }
    }

    companion object {
        private fun IrExpression.hasBranches(): Boolean = when (this) {
            is IrWhen, is IrLoop, is IrTry, is IrSuspensionPoint, is IrSuspendableExpression -> true
            else -> false
        }

        private fun IrElement.computeLocationInSourceCode(currentFile: IrFile?): Location? {
            if (currentFile == null) return null

            val moduleName: String = currentFile.module.name.asString()
            val filePath: String = currentFile.fileEntry.name

            val lineNumber: Int
            val columnNumber: Int

            when (val effectiveStartOffset = startOffsetOfFirstDenotableIrElement()) {
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

        private tailrec fun IrElement.startOffsetOfFirstDenotableIrElement(): Int = when (this) {
            is IrPackageFragment -> UNDEFINED_OFFSET
            !is IrDeclaration -> {
                // We don't generate non-denotable IR expressions in the course of partial linkage.
                startOffset
            }

            else -> if (origin is PartiallyLinkedDeclarationOrigin) {
                // There is no sense to take coordinates from the declaration that does not exist in the code.
                // Let's take the coordinates of the parent.
                parent.startOffsetOfFirstDenotableIrElement()
            } else {
                startOffset
            }
        }
    }
}
