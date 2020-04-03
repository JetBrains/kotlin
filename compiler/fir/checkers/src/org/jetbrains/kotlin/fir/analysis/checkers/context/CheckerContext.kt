/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.name.Name

abstract class CheckerContext {
    abstract val implicitReceiverStack: ImplicitReceiverStack
    abstract val containingDeclarations: List<FirDeclaration>
    abstract val sessionHolder: SessionHolder

    val session: FirSession
        get() = sessionHolder.session

    /**
     * Returns the closest to the end of context.containingDeclarations
     * T instance or null if no such item could be found.
     */
    inline fun <reified T : FirDeclaration> findClosest(): T? {
        for (it in containingDeclarations.reversed()) {
            return it as? T ?: continue
        }

        return null
    }
}

class PersistentCheckerContext(
    override val implicitReceiverStack: PersistentImplicitReceiverStack = PersistentImplicitReceiverStack(),
    override val containingDeclarations: PersistentList<FirDeclaration> = persistentListOf(),
    override val sessionHolder: SessionHolder
) : CheckerContext() {
    constructor(sessionHolder: SessionHolder) : this(
        PersistentImplicitReceiverStack(),
        persistentListOf(),
        sessionHolder
    )

    fun addImplicitReceiver(name: Name?, value: ImplicitReceiverValue<*>): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack.add(name, value),
            containingDeclarations,
            sessionHolder
        )
    }

    fun addDeclaration(declaration: FirDeclaration): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations.add(declaration),
            sessionHolder
        )
    }
}