/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.isContextParameter
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf

class VariableStorage(private val session: FirSession) {
    // This is basically a set, since it maps each key to itself. The only point of having it as a map
    // is to deduplicate equal instances with lookups. The impact of that is questionable, but whatever.
    private val realVariables: MutableMap<RealVariable, RealVariable> = HashMap()

    private val memberVariables: SetMultimap<RealVariable, RealVariable> = setMultimapOf()

    /**
     * Retrieve a [DataFlowVariable] representing the given [FirExpression]. If [fir] is a property reference,
     * return a [RealVariable], otherwise a [SyntheticVariable].
     *
     * @param createReal Whether to create a new [RealVariable] for [fir] if one has never been created before. This is a
     * performance optimization: always setting this to true is semantically correct, so it should be set to false whenever
     * this does not result in missing statements.
     *
     * @param unwrapAlias When multiple [RealVariable]s are known to have the same value in every execution, this lambda
     * can map them to some "representative" variable so that type information about all these variables is shared. It can also
     * return null to abort the entire [get] operation, making it also return null.
     *
     * @param unwrapAliasInReceivers Same as [unwrapAlias], but used when looking up [RealVariable]s for receivers of [fir]
     * if it is a qualified access expression.
     */
    fun get(
        fir: FirExpression,
        createReal: Boolean,
        unwrapAlias: (RealVariable) -> RealVariable?,
        unwrapAliasInReceivers: (RealVariable) -> RealVariable? = unwrapAlias,
    ): DataFlowVariable? {
        val unwrapped = fir.unwrapElement() ?: return null
        val isImplicit = unwrapped is FirThisReceiverExpression ||
                unwrapped.toResolvedCallableSymbol(session)?.isContextParameter() == true
        val symbol = when (unwrapped) {
            is FirWhenSubjectExpression -> unwrapped.whenRef.value.subjectVariable?.symbol
            is FirResolvedQualifier -> unwrapped.symbol?.fullyExpandedClass(session)
            is FirResolvable -> unwrapped.calleeReference.symbol
            else -> null
        }?.takeIf {
            isImplicit || it is FirClassSymbol || (it is FirVariableSymbol && it !is FirSyntheticPropertySymbol)
        }?.unwrapFakeOverridesIfNecessary() ?: return SyntheticVariable(unwrapped)

        val qualifiedAccess = unwrapped as? FirQualifiedAccessExpression
        val dispatchReceiverVar = qualifiedAccess?.dispatchReceiver?.let {
            (get(it, createReal, unwrapAliasInReceivers) ?: return null) as? RealVariable ?: return SyntheticVariable(unwrapped)
        }
        val extensionReceiverVar = qualifiedAccess?.extensionReceiver?.let {
            (get(it, createReal, unwrapAliasInReceivers) ?: return null) as? RealVariable ?: return SyntheticVariable(unwrapped)
        }
        val prototype = RealVariable(symbol, isImplicit, dispatchReceiverVar, extensionReceiverVar, unwrapped.resolvedType)
        val real = if (createReal) rememberWithKnownReceivers(prototype) else realVariables[prototype] ?: return null
        return unwrapAlias(real)
    }

    fun getKnown(variable: RealVariable): RealVariable? =
        realVariables[variable]

    /** Store a reference to a variable so that [get] can return it even if `createReal` is false. */
    fun remember(variable: RealVariable): RealVariable =
        rememberWithKnownReceivers(variable.mapReceivers(::remember))

    private fun rememberWithKnownReceivers(variable: RealVariable): RealVariable =
        realVariables.getOrPut(variable) {
            variable.dispatchReceiver?.let { memberVariables.put(it, variable) }
            variable.extensionReceiver?.let { memberVariables.put(it, variable) }
            variable
        }

    private inline fun RealVariable.mapReceivers(block: (RealVariable) -> RealVariable): RealVariable =
        RealVariable(symbol, isImplicit, dispatchReceiver?.let(block), extensionReceiver?.let(block), originalType)

    /**
     * Call a lambda with every known [RealVariable] that represents a member property of another [RealVariable].
     *
     * @param to If not null, additionally replace these variables' receivers with the new value, and pass the modified member
     * to the lambda as the second argument.
     *
     * For example, if [from] represents `x`, [to] represents `y`, and there is a known [RealVariable] representing `x.p`, then
     * [processMember] will be called that variable as the first argument and a new variable representing `y.p` as the second.
     */
    fun replaceReceiverReferencesInMembers(from: RealVariable, to: RealVariable?, processMember: (RealVariable, RealVariable?) -> Unit) {
        for (member in memberVariables[from]) {
            val remapped = to?.let { rememberWithKnownReceivers(member.mapReceivers { if (it == from) to else it }) }
            processMember(member, remapped)
        }
    }

    private tailrec fun FirExpression.unwrapElement(): FirExpression? {
        return when (this) {
            is FirWhenSubjectExpression -> (whenRef.value.takeIf { it.subjectVariable == null }?.subject ?: return this).unwrapElement()
            is FirSmartCastExpression -> originalExpression.unwrapElement()
            // Safe assignments (a?.x = b) have non-expression selectors. In this case the entire safe call
            // is not really an expression either, so we shouldn't produce any kinds of statements on it.
            // For example, saying that `(a?.x = b) != null => a != null` makes no sense, since an assignment
            // has no value in the first place, null or otherwise.
            is FirSafeCallExpression -> (selector as? FirExpression ?: return null).unwrapElement()
            is FirCheckedSafeCallSubject -> originalReceiverRef.value.unwrapElement()
            is FirCheckNotNullCall -> argument.unwrapElement()
            is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.unwrapElement()
            else -> this
        }
    }

    private fun FirBasedSymbol<*>.unwrapFakeOverridesIfNecessary(): FirBasedSymbol<*> {
        if (this !is FirCallableSymbol) return this
        // This is necessary only for sake of optimizations necessary because this is a really hot place.
        // Not having `dispatchReceiverType` means that this is a local/top-level property that can't be a fake override.
        // And at the same time, checking a field is much faster than a couple of attributes (0.3 secs at MT Full Kotlin)
        if (this.dispatchReceiverType == null) return this

        return this.unwrapFakeOverrides()
    }
}
