/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageCase.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.DeclarationId
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.DeclarationId.Companion.declarationId
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.isEffectivelyMissingLazyIrDeclaration
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.PARTIAL_LINKAGE_RUNTIME_ERROR
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.IrMessageLogger.Severity
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.compact
import java.util.*
import kotlin.properties.Delegates
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.Module as PLModule
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.File as PLFile

internal class PartiallyLinkedIrTreePatcher(
    private val builtIns: IrBuiltIns,
    private val classifierExplorer: ClassifierExplorer,
    private val stubGenerator: MissingDeclarationStubGenerator,
    private val messageLogger: IrMessageLogger
) {
    // Avoid revisiting roots that already have been visited.
    private val visitedModuleFragments = hashSetOf<IrModuleFragment>()
    private val visitedDeclarations = hashSetOf<IrDeclaration>()

    private val stdlibModule by lazy { PLModule.determineModuleFor(builtIns.anyClass.owner) }

    private inline val PLModule.shouldBeSkipped: Boolean get() = this == PLModule.SyntheticBuiltInFunctions || this == stdlibModule
    private inline val IrModuleFragment.shouldBeSkipped: Boolean get() = files.isEmpty() || name.asString() == stdlibModule.name

    fun patchModuleFragments(roots: Sequence<IrModuleFragment>) {
        roots.forEach { root ->
            if (!root.shouldBeSkipped && visitedModuleFragments.add(root)) {
                root.transformVoid(DeclarationTransformer(startingFile = null))
                root.transformVoid(ExpressionTransformer(startingFile = null))
            }
        }
    }

    fun patchDeclarations(roots: Collection<IrDeclaration>) {
        roots.forEach { root ->
            val startingFile = PLFile.determineFileFor(root)
            if (!startingFile.module.shouldBeSkipped && visitedDeclarations.add(root)) {
                root.transformVoid(DeclarationTransformer(startingFile))
                root.transformVoid(ExpressionTransformer(startingFile))
            }
        }
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
                "Expected to remove $expectedToRemove declarations in $container, but removed only $removed"
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
    private inner class DeclarationTransformer(startingFile: PLFile?) : FileAwareIrElementTransformerVoid(startingFile) {
        private val stack = ArrayDeque<DeclarationTransformerContext>()

        private fun <T : IrDeclaration> T.transformChildren(): T {
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
            // Discover the reason why the class itself is unusable.
            val unusableClass = declaration.symbol.explore()
            if (unusableClass != null) {
                // Transform the reason into the most appropriate linkage case.
                val partialLinkageCase = when (unusableClass) {
                    is ExploredClassifier.Unusable.CanBeRootCause -> UnusableClassifier(unusableClass)
                    is ExploredClassifier.Unusable.DueToOtherClassifier -> DeclarationWithUnusableClassifier(
                        declaration.symbol,
                        unusableClass.rootCause
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
                anonInitializer.body.statements += partialLinkageCase.throwLinkageError(
                    anonInitializer,
                    suppressWarningInCompilerOutput = false
                )

                // Finish processing of the current class.
                declaration.typeParameters.forEach { tp ->
                    tp.superTypes.toPartiallyLinkedMarkerTypeOrNull()?.let { newSuperType ->
                        tp.superTypes = listOf(newSuperType)
                    }
                }

                declaration.superTypes = declaration.superTypes.filter { /* filter unusable */ it.explore() == null }

                /**
                 * Remove the class in the following cases:
                 * - It is a local class (or anonymous object)
                 * - It is an inner class
                 * - It is a class without non-inner underlying classes
                 *
                 * The removal of local/inner class leads to removal of all underlying declarations including any classes declared
                 * under the removed class. In all cases that could be only inner classes that share state with their containing
                 * class and that become unusable together with the containing class.
                 *
                 * The removal of class of any other type is not performed: Such class may have nested classes that do not share
                 * their state with the containing class and not necessarily become unusable together with the containing class.
                 */
                if (declaration.isLocal || declaration.isInner || declaration.declarations.none { (it as? IrClass)?.isInner == false }) {
                    declaration.scheduleForRemoval() // Don't process underlying declarations.
                    return declaration
                }
            }

            // Process underlying declarations. Collect declarations to remove.
            return declaration.transformChildrenWithRemoval()
        }

        override fun visitConstructor(declaration: IrConstructor): IrStatement {
            // IMPORTANT: It's necessary to overwrite types. Please don't move the statement below.
            val unusableClassifierInSignature = declaration.rewriteTypesInFunction()

            val invalidConstructorDelegation = declaration.checkConstructorDelegation()

            // Compute the linkage case.
            val partialLinkageCase = if (declaration.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                MissingDeclaration(declaration.symbol)
            else
                unusableClassifierInSignature?.let { DeclarationWithUnusableClassifier(declaration.symbol, it) }
                    ?: invalidConstructorDelegation

            if (partialLinkageCase != null) {
                // Note: Block body is missing for MISSING_DECLARATION.
                val blockBody = declaration.body as? IrBlockBody
                    ?: builtIns.irFactory.createBlockBody(declaration.startOffset, declaration.endOffset).apply { declaration.body = this }

                if (invalidConstructorDelegation != null) {
                    // Drop invalid delegating constructor call. Otherwise it may break some lowerings.
                    blockBody.statements.removeIf { it is IrDelegatingConstructorCall }
                }

                // IMPORTANT: Unlike it's done for IrSimpleFunction don't clean-up statements. Insert PL linkage as the first one.
                // This is necessary to preserve anonymous initializer call and delegating constructor call in place.
                blockBody.statements.add(
                    0, partialLinkageCase.throwLinkageError(
                        declaration,
                        suppressWarningInCompilerOutput = declaration.isDirectMemberOf(unusableClassifierInSignature)
                    )
                )
            }

            return declaration.transformChildren()
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
            declaration.filterOverriddenSymbols()

            // IMPORTANT: It's necessary to overwrite types. Please don't move the statement below.
            val unusableClassifierInSignature = declaration.rewriteTypesInFunction()

            // Compute the linkage case.
            val partialLinkageCase = when (declaration.origin) {
                PartiallyLinkedDeclarationOrigin.UNIMPLEMENTED_ABSTRACT_CALLABLE_MEMBER -> UnimplementedAbstractCallable(declaration)
                PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION -> MissingDeclaration(declaration.symbol)
                else -> unusableClassifierInSignature?.let { DeclarationWithUnusableClassifier(declaration.symbol, it) }
            }

            if (partialLinkageCase != null) {
                // Note: Block body is missing for UNIMPLEMENTED_ABSTRACT_CALLABLE_MEMBER and MISSING_DECLARATION.
                val blockBody = declaration.body as? IrBlockBody
                    ?: builtIns.irFactory.createBlockBody(declaration.startOffset, declaration.endOffset).apply { declaration.body = this }

                // Clean initializer body. Don't process underlying statements.
                blockBody.statements.clear()

                // Generate IR call that throws linkage error. Report compiler warning.
                blockBody.statements += partialLinkageCase.throwLinkageError(
                    declaration,
                    suppressWarningInCompilerOutput = declaration.isDirectMemberOf(unusableClassifierInSignature)
                )

                // Don't remove inline functions, this may harm linkage in K/N backend with enabled static caches.
                if (!declaration.isInline) {
                    if (declaration.isTopLevelDeclaration) {
                        // Optimization: Remove unlinked top-level functions.
                        declaration.scheduleForRemoval()
                    } else {
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
         * Checks if there is an issue with constructor delegation.
         */
        private fun IrConstructor.checkConstructorDelegation(): InvalidConstructorDelegation? {
            if (origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                return null

            val statements = (body as? IrBlockBody)?.statements ?: return null

            val constructedClass = parentAsClass
            val constructedClassSymbol = constructedClass.symbol

            val actualSuperClassSymbol = constructedClass.superTypes.firstNotNullOfOrNull { superType ->
                val superClassSymbol = (superType as? IrSimpleType)?.classifier as? IrClassSymbol ?: return@firstNotNullOfOrNull null
                if (superClassSymbol.owner.isClass) superClassSymbol else null
            } ?: builtIns.anyClass
            val actualSuperClass = actualSuperClassSymbol.owner

            statements.forEach { statement ->
                if (statement !is IrDelegatingConstructorCall) return@forEach

                val calledConstructorSymbol = statement.symbol
                val calledConstructor = calledConstructorSymbol.owner

                val invalidConstructorDelegationFound =
                    if (calledConstructor.origin != PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION) {
                        val constructedSuperClassSymbol = calledConstructor.parentAsClass.symbol
                        constructedSuperClassSymbol != constructedClassSymbol && constructedSuperClassSymbol != actualSuperClassSymbol
                    } else {
                        // Fallback to signatures.
                        (calledConstructorSymbol.signature as? IdSignature.CommonSignature)?.let { constructorSignature ->
                            val constructedSuperClassId = DeclarationId(
                                constructorSignature.packageFqName,
                                constructorSignature.declarationFqName.substringBeforeLast('.')
                            )

                            actualSuperClass.declarationId != constructedSuperClassId
                        } ?: false
                    }

                if (invalidConstructorDelegationFound)
                    return InvalidConstructorDelegation(
                        constructorSymbol = symbol,
                        superClassSymbol = actualSuperClassSymbol,
                        unexpectedSuperClassConstructorSymbol = calledConstructorSymbol
                    )
            }

            return null
        }

        /**
         * Returns the first encountered [ExploredClassifier.Unusable].
         */
        private fun IrFunction.rewriteTypesInFunction(): ExploredClassifier.Unusable? {
            // Remember the first assignment. Ignore all subsequent.
            var result: ExploredClassifier.Unusable? by Delegates.vetoable(null) { _, oldValue, _ -> oldValue == null }

            fun IrValueParameter.fixType() {
                val newType = type.toPartiallyLinkedMarkerTypeOrNull() ?: return
                type = newType
                if (varargElementType != null) varargElementType = newType
                defaultValue = null

                result = newType.unusableClassifier
            }

            dispatchReceiverParameter?.fixType() // The dispatcher (aka this) is intentionally the first one.
            extensionReceiverParameter?.fixType()
            valueParameters.forEach { it.fixType() }

            returnType.toPartiallyLinkedMarkerTypeOrNull()?.let { newReturnType ->
                returnType = newReturnType
                result = newReturnType.unusableClassifier
            }

            typeParameters.forEach { tp ->
                tp.superTypes.toPartiallyLinkedMarkerTypeOrNull()?.let { newSuperType ->
                    tp.superTypes = listOf(newSuperType)
                    result = newSuperType.unusableClassifier
                }
            }

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
            if (overriddenSymbols.isNotEmpty()) {
                overriddenSymbols = overriddenSymbols.filterTo(ArrayList(overriddenSymbols.size)) { symbol ->
                    val owner = symbol.owner as IrDeclaration
                    owner.origin != PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION
                            // Handle the case when the overridden declaration became private.
                            && (owner as? IrDeclarationWithVisibility)?.visibility != DescriptorVisibilities.PRIVATE
                }.compact()
            }
        }

        private fun PartialLinkageCase.throwLinkageError(declaration: IrDeclaration, suppressWarningInCompilerOutput: Boolean): IrCall =
            throwLinkageError(declaration, currentFile, suppressWarningInCompilerOutput)
    }

    private inner class ExpressionTransformer(startingFile: PLFile?) : FileAwareIrElementTransformerVoid(startingFile) {
        override fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment {
            (declaration as? IrFile)?.filterUnusableAnnotations()
            return super.visitPackageFragment(declaration)
        }

        override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
            return if (declaration.origin is PartiallyLinkedDeclarationOrigin)
                declaration // There are neither expressions nor annotations to patch.
            else {
                declaration.filterUnusableAnnotations()
                super.visitDeclaration(declaration)
            }
        }

        override fun visitBlockBody(body: IrBlockBody): IrBody {
            return super.visitBlockBody(body).apply {
                (this as? IrStatementContainer)?.statements?.eliminateDeadCodeStatements()
            }
        }

        override fun visitReturn(expression: IrReturn) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(returnTargetSymbol, checkVisibility = false)
        }

        override fun visitBlock(expression: IrBlock) = expression.maybeThrowLinkageError {
            if (this is IrReturnableBlock)
                checkReferencedDeclaration(symbol, checkVisibility = false)
                    ?: checkReferencedDeclaration(inlineFunctionSymbol, checkVisibility = false)
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
                ?: checkDispatchReceiver(symbol.owner)
        }

        override fun visitConstructorCall(expression: IrConstructorCall) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkNotAbstractClass()
                ?: checkExpressionTypeArguments()
                ?: checkReferencedDeclarationType(expression.symbol.owner.parentAsClass, "regular class") { constructedClass ->
                    constructedClass.kind == ClassKind.CLASS || constructedClass.kind == ClassKind.ANNOTATION_CLASS
                }
                ?: checkDispatchReceiver(symbol.owner)
        }

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkExpressionTypeArguments()
                ?: checkReferencedDeclarationType(expression.symbol.owner.parentAsClass, "enum class") { constructedClass ->
                    constructedClass.kind == ClassKind.ENUM_CLASS || constructedClass.symbol == builtIns.enumClass
                }
                ?: checkDispatchReceiver(symbol.owner)
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkExpressionTypeArguments()
                ?: checkDispatchReceiver(symbol.owner)
        }

        override fun visitFunctionReference(expression: IrFunctionReference) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkReferencedDeclaration(reflectionTarget)
                ?: checkExpressionTypeArguments()
                ?: checkDispatchReceiver(symbol.owner)
        }

        override fun visitPropertyReference(expression: IrPropertyReference) = expression.maybeThrowLinkageError {
            checkReferencedDeclaration(symbol)
                ?: checkReferencedDeclaration(getter)
                ?: checkReferencedDeclaration(setter)
                ?: checkReferencedDeclaration(field)
                ?: checkExpressionTypeArguments()
        }

        // Never patch instance initializers. Otherwise this will break a lot of lowerings.
        override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = expression

        override fun visitExpression(expression: IrExpression) = expression.maybeThrowLinkageError { null }

        private inline fun <T : IrExpression> T.maybeThrowLinkageError(computePartialLinkageCase: T.() -> PartialLinkageCase?): IrExpression {
            // The codegen uses postorder traversal: Children are evaluated/executed before the containing expression.
            // So it's important to patch children and insert the necessary `throw IrLinkageError(...)` calls if necessary
            // before patching the containing expression itself. This would guarantee that the order of linkage errors in a program
            // would conform to the natural program execution flow.
            transformChildrenVoid()

            val partialLinkageCase = computePartialLinkageCase()
                ?: checkExpressionType(type) // Check something that is always present in every expression.
                ?: return apply { (this as? IrContainerExpression)?.statements?.eliminateDeadCodeStatements() }

            // Collect direct children if `this` isn't an expression with branches.
            val directChildren = if (!hasBranches())
                DirectChildrenStatementsCollector().also(::acceptChildrenVoid).getResult() else DirectChildren.EMPTY

            val linkageError = partialLinkageCase.throwLinkageError(element = this, currentFile, suppressWarningInCompilerOutput = false)

            return if (directChildren.statements.isNotEmpty())
                IrCompositeImpl(startOffset, endOffset, builtIns.nothingType, PARTIAL_LINKAGE_RUNTIME_ERROR).apply {
                    statements += directChildren.statements
                    if (!directChildren.hasPartialLinkageRuntimeError) statements += linkageError
                }
            else
                linkageError
        }

        private fun IrExpression.checkExpressionType(type: IrType): PartialLinkageCase? {
            return ExpressionWithUnusableClassifier(this, type.explore() ?: return null)
        }

        private fun IrMemberAccessExpression<*>.checkExpressionTypeArguments(): PartialLinkageCase? {
            // TODO: is it necessary to check that the number of type parameters matches the number of type arguments?
            return ExpressionWithUnusableClassifier(
                this,
                (0 until typeArgumentsCount).firstNotNullOfOrNull { index -> getTypeArgument(index)?.explore() } ?: return null
            )
        }

        private fun IrExpression.checkReferencedDeclaration(
            symbol: IrSymbol?,
            checkVisibility: Boolean = true
        ): PartialLinkageCase? {
            symbol ?: return null

            if (!symbol.isBound && !symbol.isPublicApi) {
                // Such symbols might not be in SymbolTable. Just bind them in place.
                stubGenerator.getDeclaration(symbol)
            }

            when (val owner = symbol.owner) {
                is IrDeclaration -> {
                    if (owner.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION
                        || (owner as? IrLazyDeclarationBase)?.isEffectivelyMissingLazyIrDeclaration() == true
                    ) {
                        return ExpressionWithMissingDeclaration(this, symbol)
                    }
                }

                else -> return null // Not a declaration.
            }

            val partialLinkageCase = when (symbol) {
                is IrClassifierSymbol -> symbol.explore()?.let { ExpressionWithUnusableClassifier(this, it) }

                is IrEnumEntrySymbol -> checkReferencedDeclaration(symbol.owner.correspondingClass?.symbol, checkVisibility = false)

                is IrPropertySymbol -> checkReferencedDeclaration(symbol.owner.getter?.symbol, checkVisibility = false)
                    ?: checkReferencedDeclaration(symbol.owner.setter?.symbol, checkVisibility = false)
                    ?: checkReferencedDeclaration(symbol.owner.backingField?.symbol, checkVisibility = false)

                else -> when (symbol) {
                    is IrFunctionSymbol -> with(symbol.owner) {
                        dispatchReceiverParameter?.type?.precalculatedUnusableClassifier()
                            ?: extensionReceiverParameter?.type?.precalculatedUnusableClassifier()
                            ?: valueParameters.firstNotNullOfOrNull { it.type.precalculatedUnusableClassifier() }
                            ?: returnType.precalculatedUnusableClassifier()
                            ?: typeParameters.firstNotNullOfOrNull { it.superTypes.precalculatedUnusableClassifier() }
                    }

                    is IrFieldSymbol -> symbol.owner.type.precalculatedUnusableClassifier()
                    is IrValueSymbol -> symbol.owner.type.precalculatedUnusableClassifier()

                    else -> null
                }?.let { unusableClassifierInReferencedDeclaration ->
                    ExpressionHasDeclarationWithUnusableClassifier(this, symbol, unusableClassifierInReferencedDeclaration)
                }
            }

            if (partialLinkageCase != null)
                return partialLinkageCase
            else if (!checkVisibility)
                return null

            // Do the minimal visibility check: Make sure that private declaration is not used outside its declaring entity.
            // This should be enough to fix KT-54469 (cases #2 and #3).

            val signature = symbol.signature
            if (signature != null
                && (!signature.isPubliclyVisible || (signature as? IdSignature.CompositeSignature)?.container is IdSignature.FileSignature)
            ) {
                // Special case: A declaration with private signature can't be referenced from another module. So nothing to check.
                return null
            }

            val declaration = symbol.owner as? IrDeclarationWithVisibility ?: return null
            val containingModule = PLModule.determineModuleFor(declaration)

            return when {
                containingModule == currentFile.module -> {
                    // OK. Used in the same module.
                    null
                }
                containingModule.shouldBeSkipped -> {
                    // Optimization: Don't check visibility of declarations in stdlib & co.
                    null
                }
                !declaration.isEffectivelyPrivate() -> {
                    // Effectively public. Nothing to check.
                    null
                }
                else -> {
                    val declaringModule = if (declaration is IrOverridableDeclaration<*> && declaration.isFakeOverride) {
                        // Compute the declaring module.
                        declaration.allOverridden()
                            .firstOrNull { !it.isFakeOverride }
                            ?.let(PartialLinkageUtils.Module.Companion::determineModuleFor)
                            ?: containingModule
                    } else
                        containingModule

                    ExpressionHasInaccessibleDeclaration(this, symbol, declaringModule, currentFile.module)
                }
            }
        }

        private fun IrType.precalculatedUnusableClassifier(): ExploredClassifier.Unusable? =
            (this as? PartiallyLinkedMarkerType)?.unusableClassifier ?: explore()

        private fun List<IrType>.precalculatedUnusableClassifier(): ExploredClassifier.Unusable? =
            firstNotNullOfOrNull { it.precalculatedUnusableClassifier() }

        private fun <D : IrDeclaration> IrExpression.checkReferencedDeclarationType(
            declaration: D,
            expectedDeclarationDescription: String,
            checkDeclarationType: (D) -> Boolean
        ): PartialLinkageCase? {
            return if (!checkDeclarationType(declaration))
                ExpressionHasWrongTypeOfDeclaration(this, declaration.symbol, expectedDeclarationDescription)
            else null
        }

        private fun <D : IrFunction> IrMemberAccessExpression<IrFunctionSymbol>.checkDispatchReceiver(
            declaration: D
        ): PartialLinkageCase? {
            val expressionHasDispatchReceiver = dispatchReceiver != null
            val declarationHasDispatchReceiver = declaration.dispatchReceiverParameter != null

            return if (expressionHasDispatchReceiver != declarationHasDispatchReceiver)
                ExpressionDispatchReceiverMismatch(this, expressionHasDispatchReceiver)
            else
                null
        }

        private fun IrConstructorCall.checkNotAbstractClass(): PartialLinkageCase? {
            val createdClass = symbol.owner.parentAsClass
            return if (createdClass.modality == Modality.ABSTRACT || createdClass.modality == Modality.SEALED)
                AbstractClassInstantiation(this, createdClass.symbol)
            else
                null
        }

        private fun <T> T.filterUnusableAnnotations() where T : IrMutableAnnotationContainer, T : IrElement {
            if (annotations.isNotEmpty()) {
                annotations = annotations.filterTo(ArrayList(annotations.size)) { annotation ->
                    val partialLinkageCase = with(annotation) {
                        checkReferencedDeclaration(symbol)
                            ?: checkExpressionTypeArguments()
                            ?: checkReferencedDeclarationType(symbol.owner.parentAsClass, "annotation class") { constructedClass ->
                                constructedClass.kind == ClassKind.ANNOTATION_CLASS
                            }
                    } ?: return@filterTo true

                    // Just log a warning. Do not throw a linkage error as this would produce broken IR.
                    partialLinkageCase.renderAndLogLinkageError(this, currentFile)

                    false // Drop it.
                }.compact()
            }
        }

        /**
         * Removes statements after the first IR p.l. error (everything after the IR p.l. error if effectively dead code and do not need
         * to be kept in the IR tree).
         */
        private fun MutableList<IrStatement>.eliminateDeadCodeStatements() {
            var hasPartialLinkageRuntimeError = false
            removeIf { statement ->
                val needToRemove = when (statement) {
                    is IrInstanceInitializerCall,
                    is IrDelegatingConstructorCall,
                    is IrEnumConstructorCall -> false // Don't remove essential constructor statements.
                    else -> hasPartialLinkageRuntimeError
                }
                hasPartialLinkageRuntimeError = hasPartialLinkageRuntimeError || statement.isPartialLinkageRuntimeError()
                needToRemove
            }
        }
    }

    private fun IrClassifierSymbol.explore(): ExploredClassifier.Unusable? = classifierExplorer.exploreSymbol(this)
    private fun IrType.explore(): ExploredClassifier.Unusable? = classifierExplorer.exploreType(this)

    private fun IrType.toPartiallyLinkedMarkerTypeOrNull(): PartiallyLinkedMarkerType? =
        explore()?.let { PartiallyLinkedMarkerType(builtIns, it) }

    private fun List<IrType>.toPartiallyLinkedMarkerTypeOrNull(): PartiallyLinkedMarkerType? =
        firstNotNullOfOrNull { it.toPartiallyLinkedMarkerTypeOrNull() }

    private fun PartialLinkageCase.renderAndLogLinkageError(element: IrElement, file: PLFile): String {
        val errorMessage = renderLinkageError()
        val locationInSourceCode = file.computeLocationForOffset(element.startOffsetOfFirstDenotableIrElement())

        messageLogger.report(Severity.WARNING, errorMessage, locationInSourceCode) // It's OK. We log it as a warning.

        return errorMessage
    }

    private fun PartialLinkageCase.throwLinkageError(element: IrElement, file: PLFile, suppressWarningInCompilerOutput: Boolean): IrCall {
        val errorMessage = if (suppressWarningInCompilerOutput)
            renderLinkageError()
        else
            renderAndLogLinkageError(element, file)

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

    private data class DirectChildren(val statements: List<IrStatement>, val hasPartialLinkageRuntimeError: Boolean) {
        companion object {
            val EMPTY = DirectChildren(emptyList(), false)
        }
    }

    /**
     * Collects direct children statements up to the first IR p.l. error (everything after the IR p.l. error
     * if effectively dead code and do not need to be kept in the IR tree).
     */
    private inner class DirectChildrenStatementsCollector : IrElementVisitorVoid {
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
        private fun IrDeclaration.isDirectMemberOf(unusableClassifier: ExploredClassifier.Unusable?): Boolean {
            val unusableClassifierSymbol = unusableClassifier?.symbol ?: return false
            val containingClassSymbol = parentClassOrNull?.symbol ?: return false
            return unusableClassifierSymbol == containingClassSymbol
        }

        private fun IrExpression.hasBranches(): Boolean = when (this) {
            is IrWhen, is IrLoop, is IrTry, is IrSuspensionPoint, is IrSuspendableExpression -> true
            else -> false
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
