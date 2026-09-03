/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirLoopTarget
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Context<T> {
    lateinit var packageFqName: FqName
    var className: FqName = FqName.ROOT
    var inLocalContext: Boolean = false
    val currentClassId: ClassId
        get() = when {
            inLocalContext -> ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, className, isLocal = true)
            else -> ClassId(packageFqName, className, isLocal = false)
        }

    var classNameBeforeLocalContext: FqName = FqName.ROOT

    val firFunctionTargets: MutableList<FirFunctionTarget> = mutableListOf()
    val calleeNamesForLambda: MutableList<Name?> = mutableListOf()

    @PrivateForInline
    val _firLabels: MutableList<FirLabel> = mutableListOf()

    @OptIn(PrivateForInline::class)
    val firLabels: List<FirLabel>
        get() = _firLabels

    /**
     * A designated `KtElement` or `LighterASTNode` object that is allowed to claim the last label in [firLabels].
     */
    @PrivateForInline
    var firLabelUserNode: Any? = null
    val firLoopTargets: MutableList<FirLoopTarget> = mutableListOf()
    val capturedTypeParameters: MutableList<StatusFirTypeParameterSymbolList> = mutableListOf()
    val arraySetArgument: MutableMap<T, FirExpression> = mutableMapOf()

    val dispatchReceiverTypesStack: MutableList<ConeClassLikeType> = mutableListOf()
    var containerIsExpect: Boolean = false

    var forceKeepingTheBodyInHeaderMode: Boolean = false

    var containingScriptSymbol: FirScriptSymbol? = null
    var containingReplSymbol: FirReplSnippetSymbol? = null

    var currentCompanionBlockOwnerOrNull: FirBasedSymbol<*>? = null

    /**
     * @param isLocal if true [symbol] will be ignored
     *
     * @see Context.containerSymbol
     * @see Context.pushContainerSymbol
     * @see Context.popContainerSymbol
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <T> withContainerSymbol(
        symbol: FirBasedSymbol<*>,
        isLocal: Boolean = false,
        block: () -> T,
    ): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        if (!isLocal) {
            pushContainerSymbol(symbol)
        }

        return try {
            block()
        } finally {
            if (!isLocal) {
                popContainerSymbol(symbol)
            }
        }
    }

    inline fun <R> withForcedLocalContext(forceKeepingTheBodyInHeaderMode: Boolean = false, block: () -> R): R {
        val oldForceKeepingTheBodyInHeaderMode = forceKeepingTheBodyInHeaderMode
        this.forceKeepingTheBodyInHeaderMode = oldForceKeepingTheBodyInHeaderMode || forceKeepingTheBodyInHeaderMode
        val oldForcedLocalContext = inLocalContext
        inLocalContext = true
        val oldClassNameBeforeLocalContext = classNameBeforeLocalContext
        if (!oldForcedLocalContext) {
            classNameBeforeLocalContext = className
        }
        val oldClassName = className
        className = FqName.ROOT
        return try {
            block()
        } finally {
            classNameBeforeLocalContext = oldClassNameBeforeLocalContext
            inLocalContext = oldForcedLocalContext
            className = oldClassName
            this.forceKeepingTheBodyInHeaderMode = oldForceKeepingTheBodyInHeaderMode
        }
    }

    /**** Class name utils ****/
    inline fun <T> withChildClassName(
        name: Name,
        isExpect: Boolean,
        forceLocalContext: Boolean = false,
        l: () -> T,
    ): T = when {
        forceLocalContext -> withForcedLocalContext {
            withChildClassNameRegardlessLocalContext(name, isExpect, l)
        }
        else -> {
            withChildClassNameRegardlessLocalContext(name, isExpect, l)
        }
    }

    inline fun <T> withChildClassNameRegardlessLocalContext(
        name: Name,
        isExpect: Boolean,
        l: () -> T,
    ): T {
        className = className.child(name)
        val previousIsExpect = containerIsExpect
        containerIsExpect = previousIsExpect || isExpect
        val dispatchReceiversNumber = dispatchReceiverTypesStack.size
        return try {
            l()
        } finally {
            require(dispatchReceiverTypesStack.size <= dispatchReceiversNumber + 1) {
                "Wrong number of ${dispatchReceiverTypesStack.size}"
            }

            if (dispatchReceiverTypesStack.size > dispatchReceiversNumber) {
                dispatchReceiverTypesStack.removeAt(dispatchReceiverTypesStack.lastIndex)
            }

            className = className.parent()
            containerIsExpect = previousIsExpect
        }
    }

    fun pushFirTypeParameters(isInnerOrLocal: Boolean, parameters: List<FirTypeParameterRef>) {
        capturedTypeParameters.add(StatusFirTypeParameterSymbolList(isInnerOrLocal, parameters.map { it.symbol }))
    }

    fun popFirTypeParameters() {
        val list = capturedTypeParameters
        list.removeAt(list.lastIndex)
    }

    fun appendOuterTypeParameters(ignoreLastLevel: Boolean, typeParameters: MutableList<FirTypeParameterRef>) {
        for (index in capturedTypeParameters.lastIndex downTo 0) {
            val element = capturedTypeParameters[index]

            if (index < capturedTypeParameters.lastIndex || !ignoreLastLevel) {
                for (capturedTypeParameter in element.list) {
                    typeParameters += buildOuterClassTypeParameterRef { symbol = capturedTypeParameter }
                }
            }

            if (!element.isInnerOrLocal) {
                break
            }
        }
    }

    inline fun <T> withCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement? = null,
        currentFirTypeParameters: List<FirTypeParameterRef>,
        block: () -> T,
    ): T {
        addCapturedTypeParameters(status, declarationSource, currentFirTypeParameters)
        return try {
            block()
        } finally {
            popFirTypeParameters()
        }
    }

    fun addCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement?,
        currentFirTypeParameters: List<FirTypeParameterRef>,
    ) {
        pushFirTypeParameters(status, currentFirTypeParameters)
    }

    inline fun withCompanionBlock(block: () -> Unit) {
        val oldValue = currentCompanionBlockOwnerOrNull
        currentCompanionBlockOwnerOrNull = containerSymbolIfAny
        try {
            block()
        } finally {
            currentCompanionBlockOwnerOrNull = oldValue
        }
    }

    inline fun <T> withContainerScriptSymbol(
        symbol: FirScriptSymbol,
        block: () -> T,
    ): T {
        require(containingScriptSymbol == null) { "Nested scripts are not supported" }
        containingScriptSymbol = symbol
        pushContainerSymbol(symbol)
        return try {
            block()
        } finally {
            popContainerSymbol(symbol)
            containingScriptSymbol = null
        }
    }

    /**
     * This property will be stored in [_containerSymbolStack] instead the next symbol.
     * If the stack is already not empty, symbols that are added on top won't be replaced.
     * The forced symbol will come into effect the next time the stack is empty.
     *
     * @see containerSymbol
     * @see pushContainerSymbol
     * @see popContainerSymbol
     */
    @set:PrivateForInline
    var forcedContainerSymbol: FirBasedSymbol<*>? = null
        set(value) {
            requireWithAttachment(field == null, { "The value cannot be reassigned" }) {
                value?.let { withFirSymbolEntry("newValue", it) }
                field?.let { withFirSymbolEntry("oldValue", it) }
            }

            field = value
        }

    /**
     * This stack is required to provide correct
     * [FirAnnotationCall.containingDeclarationSymbol][org.jetbrains.kotlin.fir.expressions.FirAnnotationCall.containingDeclarationSymbol]
     * during annotation call creation.
     *
     * @see pushContainerSymbol
     * @see popContainerSymbol
     */
    val containerSymbol: FirBasedSymbol<*> get() = _containerSymbolStack.last()
    val containerSymbolIfAny: FirBasedSymbol<*>? get() = _containerSymbolStack.lastOrNull()
    private val _containerSymbolStack: MutableList<FirBasedSymbol<*>> = mutableListOf()

    /**
     * Add [symbol] to the container symbols stack. Must be paired with [popContainerSymbol].
     *
     * @see containerSymbol
     */
    fun pushContainerSymbol(symbol: FirBasedSymbol<*>) {
        /**
         * Replace [symbol] with [forcedContainerSymbol] if it is the first invocation of [pushContainerSymbol] in the stack
         */
        val containerSymbol = forcedContainerSymbol?.takeIf { _containerSymbolStack.isEmpty() } ?: symbol
        _containerSymbolStack += containerSymbol
    }

    /**
     * Remove [symbol] from the container symbols stack. Must be called after corresponding [pushContainerSymbol].
     *
     * @see containerSymbol
     */
    fun popContainerSymbol(symbol: FirBasedSymbol<*>) {
        /**
         * The counterpart of [pushContainerSymbol] logic
         */
        val removed = _containerSymbolStack.removeLast()
        val containerSymbol = forcedContainerSymbol?.takeIf { _containerSymbolStack.isEmpty() } ?: symbol
        checkWithAttachment(removed === containerSymbol, { "Inconsistent declaration stack" }) {
            withFirSymbolEntry("expected", containerSymbol)
            withFirSymbolEntry("actual", removed)
            if (symbol != containerSymbol) {
                withFirSymbolEntry("replaced symbol", symbol)
            }

            withEntry("stack", _containerSymbolStack.asReversed().toString())
        }
    }

    /**
     * Gets the last label that was added or null if the current node does not have permission to use the label.
     */
    @OptIn(PrivateForInline::class)
    fun getLastLabel(currentNode: Any): FirLabel? {
        if (this.firLabelUserNode == currentNode) return firLabels.last()
        return null
    }

    @OptIn(PrivateForInline::class)
    fun addNewLabel(label: FirLabel) {
        _firLabels += label
    }

    @OptIn(PrivateForInline::class)
    fun setNewLabelUserNode(useNode: Any?) {
        this.firLabelUserNode = useNode
    }

    @OptIn(PrivateForInline::class)
    fun dropLastLabel() {
        _firLabels.removeLast()
        firLabelUserNode = null
    }

    inline fun <T> withNewLabel(label: FirLabel, userNode: Any?, block: () -> T): T {
        addNewLabel(label)
        setNewLabelUserNode(userNode)
        try {
            return block()
        } finally {
            dropLastLabel()
        }
    }

    /**
     * Forwards the permission to use the last label to a different node if the current user node has the permission.
     */
    @OptIn(PrivateForInline::class)
    fun forwardLabelUsagePermission(currentUserNode: Any, newUserNode: Any?) {
        if (currentUserNode == firLabelUserNode) {
            firLabelUserNode = newUserNode
        }
    }

    data class StatusFirTypeParameterSymbolList(val isInnerOrLocal: Boolean, val list: List<FirTypeParameterSymbol> = listOf())
}
