/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.lifetime

import com.intellij.openapi.project.Project
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KaLifetimeTracker

internal class KaBaseLifetimeTracker : KaLifetimeTracker {
    private val lifetimeOwnersStack = ThreadLocal.withInitial<PersistentList<KaLifetimeToken>> { persistentListOf() }

    override val currentToken: KaLifetimeToken? get() = lifetimeOwnersStack.get().lastOrNull()

    fun beforeEnteringAnalysis(session: KaSession) {
        lifetimeOwnersStack.set(lifetimeOwnersStack.get().add(session.token))
    }

    fun afterLeavingAnalysis(session: KaSession) {
        val stack = lifetimeOwnersStack.get()
        val last = stack.last()
        check(last == session.token)
        lifetimeOwnersStack.set(stack.removeAt(stack.lastIndex))
    }

    companion object {
        fun getInstance(project: Project): KaBaseLifetimeTracker =
            KaLifetimeTracker.getInstance(project) as? KaBaseLifetimeTracker
                ?: error("Expected ${KaBaseLifetimeTracker::class.simpleName} to be registered for ${KaLifetimeTracker::class.simpleName}.")
    }
}
