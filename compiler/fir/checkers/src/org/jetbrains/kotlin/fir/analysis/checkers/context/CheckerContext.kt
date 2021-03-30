/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.PersistentImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.name.Name

abstract class CheckerContext {
    abstract val implicitReceiverStack: ImplicitReceiverStack
    abstract val containingDeclarations: List<FirDeclaration>
    abstract val qualifiedAccesses: List<FirQualifiedAccess>
    abstract val getClassCalls: List<FirGetClassCall>
    abstract val sessionHolder: SessionHolder
    abstract val returnTypeCalculator: ReturnTypeCalculator
    abstract val suppressedDiagnostics: Set<String>
    abstract val allInfosSuppressed: Boolean
    abstract val allWarningsSuppressed: Boolean
    abstract val allErrorsSuppressed: Boolean

    val session: FirSession
        get() = sessionHolder.session

    /**
     * Returns the closest to the end of context.containingDeclarations
     * instance of type [T] or null if no such item could be found.
     */
    inline fun <reified T : FirDeclaration> findClosest(): T? {
        for (it in containingDeclarations.asReversed()) {
            return it as? T ?: continue
        }

        return null
    }

    /**
     * Same as the one without specific [check]. For some cases, an instance of type [T] isn't good enough. E.g., property accessor is
     * either getter or setter, but a type-based search could return, say, the closest setter, while we want to keep searching for a getter.
     */
    inline fun <reified T : FirDeclaration> findClosest(check: (T) -> Boolean): T? {
        for (it in containingDeclarations.asReversed()) {
            return (it as? T)?.takeIf(check) ?: continue
        }

        return null
    }

    abstract fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): PersistentCheckerContext
}

class PersistentCheckerContext private constructor(
    override val implicitReceiverStack: PersistentImplicitReceiverStack,
    override val containingDeclarations: PersistentList<FirDeclaration>,
    override val qualifiedAccesses: PersistentList<FirQualifiedAccess>,
    override val getClassCalls: PersistentList<FirGetClassCall>,
    override val sessionHolder: SessionHolder,
    override val returnTypeCalculator: ReturnTypeCalculator,
    override val suppressedDiagnostics: PersistentSet<String>,
    override val allInfosSuppressed: Boolean,
    override val allWarningsSuppressed: Boolean,
    override val allErrorsSuppressed: Boolean
) : CheckerContext() {
    constructor(sessionHolder: SessionHolder, returnTypeCalculator: ReturnTypeCalculator) : this(
        PersistentImplicitReceiverStack(),
        persistentListOf(),
        persistentListOf(),
        persistentListOf(),
        sessionHolder,
        returnTypeCalculator,
        persistentSetOf(),
        allInfosSuppressed = false,
        allWarningsSuppressed = false,
        allErrorsSuppressed = false
    )

    fun addImplicitReceiver(name: Name?, value: ImplicitReceiverValue<*>): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack.add(name, value),
            containingDeclarations,
            qualifiedAccesses,
            getClassCalls,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    fun addDeclaration(declaration: FirDeclaration): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations.add(declaration),
            qualifiedAccesses,
            getClassCalls,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    fun addQualifiedAccess(qualifiedAccess: FirQualifiedAccess): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations,
            qualifiedAccesses.add(qualifiedAccess),
            getClassCalls,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    fun addGetClassCall(getClassCall: FirGetClassCall): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations,
            qualifiedAccesses,
            getClassCalls.add(getClassCall),
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    override fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): PersistentCheckerContext {
        if (diagnosticNames.isEmpty()) return this
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations,
            qualifiedAccesses,
            getClassCalls,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics.addAll(diagnosticNames),
            this.allInfosSuppressed || allInfosSuppressed,
            this.allWarningsSuppressed || allWarningsSuppressed,
            this.allErrorsSuppressed || allErrorsSuppressed
        )
    }
}
