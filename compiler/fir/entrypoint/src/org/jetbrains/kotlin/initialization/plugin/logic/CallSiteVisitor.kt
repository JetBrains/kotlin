/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.initialization.plugin.logic

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.initialization.plugin.model.AccessibleIndex
import org.jetbrains.kotlin.initialization.plugin.model.DefaultedFunctionIndex
import org.jetbrains.kotlin.initialization.plugin.model.DependencyNodeIndex
import org.jetbrains.kotlin.initialization.plugin.model.DependencyNodeIndex.Companion.enclosingEntity
import org.jetbrains.kotlin.initialization.plugin.model.FunctionIndex
import org.jetbrains.kotlin.initialization.plugin.model.PropertyIndex
import org.jetbrains.kotlin.initialization.plugin.dsl.DependencyGraphBuilder
import org.jetbrains.kotlin.initialization.plugin.model.EnclosingEntity
import org.jetbrains.kotlin.initialization.plugin.model.EnclosingEntity.Companion.asClassEntity
import org.jetbrains.kotlin.initialization.plugin.model.EnclosingEntity.Companion.asEnumEntryEntity
import org.jetbrains.kotlin.initialization.plugin.model.EnclosingEntity.Companion.asFileEntity
import org.jetbrains.kotlin.initialization.plugin.model.EnclosingEntity.Companion.asObjectEntity
import org.jetbrains.kotlin.initialization.plugin.model.EnclosingEntity.Companion.isNotPrivate
import org.jetbrains.kotlin.initialization.plugin.model.EnclosingEntity.Companion.parentEnclosingEntityOrSelf
import org.jetbrains.kotlin.initialization.plugin.util.contains
import org.jetbrains.kotlin.initialization.plugin.util.endInitializationIndex
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrSuspendableExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.isFunctionalTypeInvoke
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import kotlin.collections.forEach
import kotlin.let

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class CallSiteVisitor(
    private val module: IrModuleFragment,
    private val visitedFiles: Set<IrFileSymbol>,
    private val graphBuilder: DependencyGraphBuilder,
) : IrVisitor<Unit, CallSiteVisitor.CallSiteVisitContext>() {

    data class CallSiteVisitContext(
        val accessingNode: DependencyNodeIndex,
        val accessingEntity: EnclosingEntity<*>? = accessingNode.enclosingEntity,
        val materializeOnlyConstructorArguments: Boolean = false
    )

    override fun visitElement(element: IrElement, data: CallSiteVisitContext): Unit = Unit

    private inline fun <D : IrDeclaration> D.visit(
        data: CallSiteVisitContext,
        crossinline symbolSupplier: (D) -> IrBindableSymbol<*, D>,
        crossinline block: context(CallSiteVisitContext, D) DependencyGraphBuilder.() -> Unit
    ) = when {
        symbolSupplier(this) in module -> context(data, this@visit) { graphBuilder.block() }
        else -> {}
    }

    private inline fun <E : IrElement> E.visit(
        data: CallSiteVisitContext,
        crossinline block: context(CallSiteVisitContext, E) DependencyGraphBuilder.() -> Unit
    ) = context(data, this@visit) { graphBuilder.block() }

    context(context: CallSiteVisitContext)
    private fun IrElement.visitRecursively() = accept(this@CallSiteVisitor, context)

    private val <D : IrDeclaration> IrBindableSymbol<*, D>.inVisitedFiles: Boolean
        get() = owner.fileOrNull?.let { it.symbol in visitedFiles } ?: false

    context(context: CallSiteVisitContext, reference: IrExpression?)
    private fun DependencyGraphBuilder.referenceNode(node: AccessibleIndex) {
        node.buildNode()
        context.accessingNode references node
        val possiblyInitializedEndNode = node.lazilyInitialized?.endInitializationIndex ?: return
        if (context.accessingEntity?.parentEnclosingEntityOrSelf?.let { it != possiblyInitializedEndNode.enclosingEntity } ?: true) {
            possiblyInitializedEndNode.buildNode()
            possiblyInitializedEndNode mayHappenBefore context.accessingNode
        }
    }

    context(context: CallSiteVisitContext, callSite: IrExpression?)
    private fun DependencyGraphBuilder.callNode(node: FunctionIndex<*>) {
        node.buildNode()
        if (!node.symbol.inVisitedFiles) node.symbol.postponeFileEntity()
        context.accessingNode calls node
        if (node !is FunctionIndex.Constructor) {
            val enclosingEntity = node.lazilyInitialized?.parentEnclosingEntityOrSelf ?: return
            val possiblyInitializedEndNode = enclosingEntity.endInitializationIndex
            possiblyInitializedEndNode mayHappenBefore node
        }
    }

    override fun visitProperty(declaration: IrProperty, data: CallSiteVisitContext): Unit = declaration.visit(data) {
        // Visit only the initializer
        declaration.backingField?.visitRecursively()
    }

    override fun visitField(declaration: IrField, data: CallSiteVisitContext): Unit = declaration.visit(data, IrField::symbol) {
        declaration.initializer?.visitRecursively()
    }

    override fun visitBlock(expression: IrBlock, data: CallSiteVisitContext): Unit = expression.visit(data) {
        expression.statements.forEach { stmt -> stmt.visitRecursively() }
    }

    override fun visitBlockBody(body: IrBlockBody, data: CallSiteVisitContext): Unit = body.visit(data) {
        body.statements.forEach { it.visitRecursively() }
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: CallSiteVisitContext): Unit = body.visit(data) {
        body.expression.visitRecursively()
    }

    context(context: CallSiteVisitContext)
    private val <D : IrFunction> IrBindableSymbol<*, D>.defaultParametersIfAny: Pair<DefaultedFunctionIndex<*>, List<IrValueParameterSymbol>>?
        get() = when {
            context.accessingNode is DefaultedFunctionIndex<*> && context.accessingNode.functionIndex.symbol == this ->
                context.accessingNode to context.accessingNode.defaultParameters.mapNotNull { it.closestOverriddenDefaultParameter }
            else -> null
        }

    override fun visitFunction(declaration: IrFunction, data: CallSiteVisitContext): Unit = Unit

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: CallSiteVisitContext): Unit =
        declaration.visit(data, IrSimpleFunction::symbol) {
            val symbol = declaration.symbol
            symbol.defaultParametersIfAny?.let { [defaultedIndex, defaultParameters] ->
                // Add the known default values for missing parameters to the mapping
                defaultParameters.forEach { it.owner.defaultValue?.visitRecursively() }
                // Build the original function (with a non-existent call-site)
                context(null) { callNode(defaultedIndex.functionIndex) }
            } ?: declaration.body?.visitRecursively()
        }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: CallSiteVisitContext): Unit =
        declaration.visit(data, IrAnonymousInitializer::symbol) { declaration.body.visitRecursively() }

    override fun visitConstructor(declaration: IrConstructor, data: CallSiteVisitContext): Unit =
        declaration.visit(data, IrConstructor::symbol) {
            declaration.symbol.defaultParametersIfAny?.let { [defaultedNode, defaultParameters] ->
                defaultParameters.forEach { it.owner.defaultValue?.visitRecursively() }
                context(null) { callNode(defaultedNode.functionIndex) }
                return@visit
            }
            if (declaration.isPrimary && !data.materializeOnlyConstructorArguments) {
                declaration.parentClassOrNull?.asClassEntity()?.let { classEntity ->
                    classEntity.endInitializationIndex mayHappenBefore data.accessingNode
                }
            }
            val materializeEverythingContext = CallSiteVisitContext(data.accessingNode)
            declaration.body?.statements?.forEach { statement ->
                when (statement) {
                    is IrDelegatingConstructorCall -> statement.visitRecursively()
                    is IrBlock -> {
                        context(materializeEverythingContext) {
                            statement.statements.dropLast(1).forEach { it.visitRecursively() }
                        }
                        when (val last = statement.statements.lastOrNull()) {
                            null -> {}
                            is IrDelegatingConstructorCall -> last.visitRecursively()
                            else -> context(materializeEverythingContext) { last.visitRecursively() }
                        }
                    }
                    else -> context(materializeEverythingContext) { statement.visitRecursively() }
                }
            }
        }

    context(context: CallSiteVisitContext, access: A)
    private inline fun <D : IrOverridableDeclaration<S>, S : IrBindableSymbol<*, D>, A : IrDeclarationReference> DependencyGraphBuilder.accessNode(
        symbol: S,
        dispatchReceiverSupplier: D.(A) -> IrExpression? = { null },
        extensionReceiverSupplier: D.(A) -> IrExpression? = { null },
        superQualifierSupplier: (A) -> IrClassSymbol?,
        crossinline staticAccess: context(CallSiteVisitContext, A) DependencyGraphBuilder.(S, EnclosingEntity<*>) -> Unit,
        crossinline instanceAccess: context(CallSiteVisitContext, A) DependencyGraphBuilder.(S) -> Unit,
    ) {
        // If the callable is an extension, visit the extension receiver for dependencies
        symbol.owner.extensionReceiverSupplier(access)?.visitRecursively()

        symbol.realOverridden().filter { it.owner.modality != Modality.ABSTRACT }.distinct().forEach { symbol ->
            // Compute the node to this callable based on the access' dispatch receiver
            when (val receiver = symbol.owner.dispatchReceiverSupplier(access)) {
                // `super.` access
                null if superQualifierSupplier(access) != null ->
                    context.accessingEntity?.let { staticAccess(symbol, it) } ?: instanceAccess(symbol)
                // top-level access
                null if symbol.owner.isTopLevel -> symbol.owner.fileOrNull?.asFileEntity()?.let { staticAccess(symbol, it) }
                // `this.` access (implicit or otherwise)
                is IrGetValue if (receiver.symbol.owner.origin == IrDeclarationOrigin.INSTANCE_RECEIVER || receiver.origin == IrStatementOrigin.IMPLICIT_ARGUMENT) ->
                    context.accessingEntity?.let { staticAccess(symbol, it) } ?: instanceAccess(symbol)
                // `A.` qualifier access
                is IrGetObjectValue -> {
                    val enclosingEntity = receiver.symbol.asObjectEntity() ?: return@forEach
                    staticAccess(symbol, enclosingEntity)
                }
                // `E.ENTRY.` enum entry access
                is IrGetEnumValue -> staticAccess(symbol, receiver.symbol.asEnumEntryEntity())
                // `e.` arbitrary access
                is IrDeclarationReference -> {
                    // Receiver dependencies must be connected to the accessing node first
                    receiver.visitRecursively()
                    instanceAccess(symbol)
                }
                else -> {}
            }
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: CallSiteVisitContext): Unit = expression.visit(data) {
        if (expression.symbol !in module) return@visit
        val objectEntity = expression.symbol.asObjectEntity() ?: return@visit
        if (objectEntity.isNotPrivate) referenceNode(objectEntity.beginInitializationIndex)
        if (!objectEntity.symbol.inVisitedFiles) objectEntity.symbol.postponeFileEntity()
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: CallSiteVisitContext): Unit = expression.visit(data) {
        if (expression.symbol !in module) return@visit
        val enumEntryEntity = expression.symbol.asEnumEntryEntity()
        referenceNode(enumEntryEntity.beginInitializationIndex)
        if (enumEntryEntity.symbol.inVisitedFiles) enumEntryEntity.symbol.postponeFileEntity()
    }

    private fun FunctionIndex<*>.defaultedOrSelf(parameters: Set<IrValueParameterSymbol>): FunctionIndex<*> = when (this) {
        is DefaultedFunctionIndex -> this // ignore the input parameters for safety
        else -> if (parameters.isNotEmpty()) DefaultedFunctionIndex(this, parameters) else this
    }

    /**
     * Visits arguments of the given function call
     */
    context(context: CallSiteVisitContext, functionCall: T)
    private fun <T : IrFunctionAccessExpression> DependencyGraphBuilder.visitArguments(symbol: IrFunctionSymbol) {
        symbol.owner.parameters.zip(functionCall.arguments) { parameter, argument ->
            if (parameter.kind == IrParameterKind.DispatchReceiver || parameter.kind == IrParameterKind.ExtensionReceiver) return@zip
            if (parameter.isInlineParameter() && argument is IrFunctionExpression) {
                callNode(FunctionIndex.Closure(argument.function.symbol))
            } else {
                argument?.visitRecursively()
            }
        }
    }

    private fun IrCall.propertyAccessFromReceiver(): Pair<IrPropertySymbol, IrCall>? {
        val receiver = (symbol.owner.parameters.find { it.kind == IrParameterKind.ExtensionReceiver }?.let(arguments::get)
            ?: dispatchReceiver) as? IrCall
            ?: return null
        // Consider only getters of initialized vals
        return receiver.symbol.owner.takeIf(IrSimpleFunction::isGetter)
            ?.correspondingPropertySymbol?.takeIf { !it.owner.isVar && it.owner.backingField != null }
            ?.let { it to receiver }
    }

    override fun visitCall(expression: IrCall, data: CallSiteVisitContext): Unit =
        expression.visit(data) {
            val symbol = expression.symbol
            visitArguments(symbol)
            if (symbol !in module) return@visit
            when {
                // Special case for initialized properties with a closure
                symbol.isFunctionalTypeInvoke -> expression.propertyAccessFromReceiver()?.let { [property, access] ->
                    context(access) {
                        // Consider only (initialized) properties with functional type
                        if (property.owner.getter?.returnType?.isFunctionOrKFunction()?.not() ?: false) return@let
                        accessNode(
                            symbol = property,
                            dispatchReceiverSupplier = { it.dispatchReceiver },
                            // Only initialized properties so no extension receivers
                            superQualifierSupplier = { it.superQualifierSymbol },
                            staticAccess = staticAccess@{ propertySymbol, receiverEntity ->
                                val propertyNode = PropertyIndex(propertySymbol, receiverEntity)
                                val closure = propertyNode.initializedClosure ?: return@staticAccess
                                callNode(closure)
                                if (!propertySymbol.inVisitedFiles) symbol.postponeFileEntity()
                            },
                            instanceAccess = instanceAccess@{ propertySymbol ->
                                val propertyNode = PropertyIndex(propertySymbol)
                                // Early return in case there's initialized closure
                                val closure = propertyNode.initializedClosure ?: return@instanceAccess
                                // Here the property is directly accessed and it is immediately invoked, so no aliasing
                                val constructedClass = propertySymbol.owner.parentClassOrNull?.symbol ?: return@instanceAccess
                                constructedClass.postponeInitSubgraph()
                                val endNode = constructedClass.endInitializationIndex
                                endNode.buildNode()
                                callNode(closure)
                                endNode mayHappenBefore closure
                                if (!propertySymbol.inVisitedFiles) symbol.postponeFileEntity()
                            }
                        )
                    }
                    if (!property.inVisitedFiles) property.postponeFileEntity()
                }
                // General case of function calls and (val) property accesses
                else -> {
                    val defaultParameters = symbol.owner.parameters.zip(expression.arguments) { parameter, argument ->
                        if (argument == null) parameter.symbol else null
                    }.filterNotNull().toSet()

                    when (val property = symbol.owner.correspondingPropertySymbol) {
                        // Calling an actual function
                        null -> accessNode(
                            symbol = symbol,
                            dispatchReceiverSupplier = { it.dispatchReceiver },
                            extensionReceiverSupplier = { access ->
                                parameters.find { it.kind == IrParameterKind.ExtensionReceiver }?.indexInParameters?.let(access.arguments::get)
                            },
                            superQualifierSupplier = IrCall::superQualifierSymbol,
                            staticAccess = staticAccess@{ functionSymbol, receiverEntity ->
                                val node = FunctionIndex.MemberFunction(functionSymbol, receiverEntity).defaultedOrSelf(defaultParameters)
                                callNode(node)
                                if (!functionSymbol.inVisitedFiles) symbol.postponeFileEntity()
                            },
                            instanceAccess = instanceAccess@{ functionSymbol ->
                                val constructedClass = symbol.owner.parentClassOrNull?.symbol ?: return@instanceAccess
                                constructedClass.postponeInitSubgraph()
                                val endNode = constructedClass.endInitializationIndex
                                endNode.buildNode()
                                val functionNode = FunctionIndex.MemberFunction(functionSymbol).defaultedOrSelf(defaultParameters)
                                callNode(functionNode)
                                endNode mayHappenBefore functionNode
                                if (!functionSymbol.inVisitedFiles) symbol.postponeFileEntity()
                            }
                        )
                        // Calling a property getter (default or otherwise), invariant: cannot have default parameters
                        else if !property.owner.isVar && symbol.owner.isGetter && defaultParameters.isEmpty() -> accessNode(
                            symbol = property,
                            dispatchReceiverSupplier = { it.dispatchReceiver },
                            // Only initialized properties so no extension receivers
                            superQualifierSupplier = IrCall::superQualifierSymbol,
                            staticAccess = staticAccess@{ propertySymbol, receiverEntity ->
                                val propertyNode = PropertyIndex(propertySymbol, receiverEntity)
                                when {
                                    propertySymbol.owner.getter?.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR && propertyNode.hasInitializer ->
                                        referenceNode(propertyNode)
                                    else -> propertyNode.getter?.let { callNode(it) }
                                }
                                if (!propertySymbol.inVisitedFiles) symbol.postponeFileEntity()
                            },
                            instanceAccess = instanceAccess@{ propertySymbol ->
                                val constructedClass = symbol.owner.parentClassOrNull?.symbol ?: return@instanceAccess
                                constructedClass.postponeInitSubgraph()
                                val endNode = constructedClass.endInitializationIndex
                                endNode.buildNode()
                                val propertyNode = PropertyIndex(propertySymbol)
                                when {
                                    propertySymbol.owner.getter?.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR && propertyNode.hasInitializer -> {
                                        referenceNode(propertyNode)
                                        endNode mayHappenBefore data.accessingNode
                                    }
                                    else -> propertyNode.getter?.let {
                                        callNode(it)
                                        endNode mayHappenBefore it
                                    }
                                }
                                if (!propertySymbol.inVisitedFiles) symbol.postponeFileEntity()
                            }
                        )
                        else -> {}
                    }
                }
            }
            // Just in case the symbol is a fake override and was not added during access resolution,
            // we still want to explore the declarations of its containing file
            if (!symbol.inVisitedFiles) symbol.postponeFileEntity()
        }

    override fun visitConstructorCall(expression: IrConstructorCall, data: CallSiteVisitContext): Unit = expression.visit(data) {
        val symbol = expression.symbol
        visitArguments(symbol)
        if (symbol !in module) return@visit
        // Prevent from creating dependencies for nested enclosing entities that extend their outer class, since it creates unwanted cycles
        if (symbol.owner.constructedClass.asClassEntity() == data.accessingEntity?.parentEnclosingEntity) return@visit
        val defaultParameters = symbol.owner.parameters.zip(expression.arguments) { parameter, argument ->
            if (argument == null) parameter.symbol else null
        }.filterNotNull().toSet()
        callNode(FunctionIndex.Constructor(symbol).defaultedOrSelf(defaultParameters))
        if (!symbol.inVisitedFiles) symbol.postponeFileEntity()
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: CallSiteVisitContext): Unit =
        expression.visit(data) {
            val symbol = expression.symbol
            visitArguments(symbol)
            if (symbol !in module) return@visit
            val defaultParameters = symbol.owner.parameters.zip(expression.arguments) { parameter, argument ->
                if (argument == null) parameter.symbol else null
            }.filterNotNull().toSet()
            if (data.materializeOnlyConstructorArguments) {
                // Default parameters are independent expressions
                context(CallSiteVisitContext(data.accessingNode)) {
                    defaultParameters.forEach { it.closestOverriddenDefaultParameter?.owner?.defaultValue?.visitRecursively() }
                }
                // The only way to properly materialize the edges to the accessing node is to fully recurse into the construction call chain
                symbol.owner.visitRecursively()
            } else {
                callNode(FunctionIndex.Constructor(symbol).defaultedOrSelf(defaultParameters))
            }
        }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: CallSiteVisitContext) = expression.visit(data) {
        visitArguments(expression.symbol)
    }

    override fun visitCatch(aCatch: IrCatch, data: CallSiteVisitContext): Unit = aCatch.visit(data) {
        aCatch.result.visitRecursively()
    }

    override fun visitBranch(branch: IrBranch, data: CallSiteVisitContext): Unit = branch.visit(data) {
        branch.condition.visitRecursively()
        branch.result.visitRecursively()
    }

    override fun visitLoop(loop: IrLoop, data: CallSiteVisitContext): Unit = loop.visit(data) {
        loop.condition.visitRecursively()
        loop.body?.visitRecursively()
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: CallSiteVisitContext): Unit = expression.visit(data) {
        expression.argument.visitRecursively()
    }

    override fun visitVararg(expression: IrVararg, data: CallSiteVisitContext): Unit = expression.visit(data) {
        expression.elements.forEach { it.visitRecursively() }
    }

    override fun visitReturn(expression: IrReturn, data: CallSiteVisitContext): Unit = expression.visit(data) {
        expression.value.visitRecursively()
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: CallSiteVisitContext): Unit = spread.visit(data) {
        spread.expression.visitRecursively()
    }

    override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: CallSiteVisitContext): Unit =
        expression.visit(data) {
            expression.suspensionPointId.visitRecursively()
            expression.result.visitRecursively()
        }

    override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: CallSiteVisitContext): Unit = expression.visit(data) {
        expression.result.visitRecursively()
        expression.resumeResult.visitRecursively()
    }

    override fun visitThrow(expression: IrThrow, data: CallSiteVisitContext): Unit = expression.visit(data) {
        expression.value.visitRecursively()
    }

    override fun visitTry(aTry: IrTry, data: CallSiteVisitContext): Unit = aTry.visit(data) {
        aTry.tryResult.visitRecursively()
        aTry.catches.forEach { it.visitRecursively() }
        aTry.finallyExpression?.visitRecursively()
    }

    override fun visitWhen(expression: IrWhen, data: CallSiteVisitContext): Unit = expression.visit(data) {
        expression.branches.forEach { it.visitRecursively() }
    }
}
