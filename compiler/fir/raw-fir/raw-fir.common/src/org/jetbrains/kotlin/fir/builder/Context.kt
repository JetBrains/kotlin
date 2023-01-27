/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtSourceElementKind
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirLoopTarget
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class Context<T> {
    lateinit var packageFqName: FqName
    var className: FqName = FqName.ROOT
    var forcedLocalContext: Boolean = false
    val inLocalContext get() = forcedLocalContext || firFunctionTargets.isNotEmpty()
    val currentClassId get() = ClassId(packageFqName, className, inLocalContext)

    val firFunctionTargets = mutableListOf<FirFunctionTarget>()
    val calleeNamesForLambda = mutableListOf<Name>()

    @PrivateForInline
    val _firLabels = mutableListOf<FirLabel>()

    @OptIn(PrivateForInline::class)
    val firLabels: List<FirLabel>
        get() = _firLabels

    /**
     * A designated `KtElement` or `LighterASTNode` object that is allowed to claim the last label in [firLabels].
     */
    @PrivateForInline
    var firLabelUserNode: Any? = null
    val firLoopTargets = mutableListOf<FirLoopTarget>()
    val capturedTypeParameters = mutableListOf<StatusFirTypeParameterSymbolList>()
    val arraySetArgument = mutableMapOf<T, FirExpression>()

    var forcedElementSourceKind: KtSourceElementKind? = null
    val dispatchReceiverTypesStack = mutableListOf<ConeClassLikeType>()
    var containerIsExpect: Boolean = false
    var containingFileSymbol: FirFileSymbol? = null

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
