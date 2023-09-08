/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtAnalysisApiInternals::class, KtAllowProhibitedAnalyzeFromWriteAction::class)

package org.jetbrains.kotlin.analysis.api.lifetime

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import kotlin.reflect.KClass

public class KtReadActionConfinementLifetimeToken(project: Project) : KtLifetimeToken() {
    private val modificationTracker = project.createProjectWideOutOfBlockModificationTracker()
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    override fun isValid(): Boolean {
        return onCreatedTimeStamp == modificationTracker.modificationCount
    }

    override fun getInvalidationReason(): String {
        if (onCreatedTimeStamp != modificationTracker.modificationCount) return "PSI has changed since creation"
        error("Getting invalidation reason for valid validity token")
    }

    override fun isAccessible(): Boolean {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread && !allowOnEdt.get()) return false
        if (application.isWriteAccessAllowed && !allowFromWriteAction.get()) return false
        if (KtAnalysisAllowanceManager.resolveIsForbiddenInActionWithName.get() != null) return false
        if (!application.isReadAccessAllowed) return false
        if (!KtReadActionConfinementLifetimeTokenFactory.isInsideAnalysisContext()) return false
        if (KtReadActionConfinementLifetimeTokenFactory.currentToken() != this) return false
        return true
    }

    override fun getInaccessibilityReason(): String {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread && !allowOnEdt.get()) return "Called in EDT thread"
        if (application.isWriteAccessAllowed && !allowFromWriteAction.get()) return "Called from write action"
        if (!application.isReadAccessAllowed) return "Called outside read action"
        KtAnalysisAllowanceManager.resolveIsForbiddenInActionWithName.get()?.let { actionName ->
            return "Resolve is forbidden in $actionName"
        }
        if (!KtReadActionConfinementLifetimeTokenFactory.isInsideAnalysisContext()) return "Called outside analyse method"
        if (KtReadActionConfinementLifetimeTokenFactory.currentToken() != this) return "Using KtLifetimeOwner from previous analysis"

        error("Getting inaccessibility reason for validity token when it is accessible")
    }


    public companion object {
        @KtAnalysisApiInternals
        public val allowOnEdt: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

        @KtAnalysisApiInternals
        @KtAllowProhibitedAnalyzeFromWriteAction
        public val allowFromWriteAction: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    }

    public override val factory: KtLifetimeTokenFactory = KtReadActionConfinementLifetimeTokenFactory
}

public object KtReadActionConfinementLifetimeTokenFactory : KtLifetimeTokenFactory() {
    override val identifier: KClass<out KtLifetimeToken> = KtReadActionConfinementLifetimeToken::class

    override fun create(project: Project): KtLifetimeToken = KtReadActionConfinementLifetimeToken(project)

    override fun beforeEnteringAnalysisContext(token: KtLifetimeToken) {
        lifetimeOwnersStack.set(lifetimeOwnersStack.get().add(token))
    }

    override fun afterLeavingAnalysisContext(token: KtLifetimeToken) {
        val stack = lifetimeOwnersStack.get()
        val last = stack.last()
        check(last == token)
        lifetimeOwnersStack.set(stack.removeAt(stack.lastIndex))
    }

    private val lifetimeOwnersStack = ThreadLocal.withInitial<PersistentList<KtLifetimeToken>> { persistentListOf() }

    internal fun isInsideAnalysisContext() = lifetimeOwnersStack.get().isNotEmpty()

    internal fun currentToken() = lifetimeOwnersStack.get().last()
}

@RequiresOptIn("Analysis should be prohibited to be ran from write action, otherwise it may cause IDE freezes and incorrect behavior in some cases")
private annotation class KtAllowProhibitedAnalyzeFromWriteAction

/**
 * @see KtAnalysisSession
 * @see KtReadActionConfinementLifetimeToken
 */
@KtAllowAnalysisOnEdt
public inline fun <T> allowAnalysisOnEdt(action: () -> T): T {
    if (KtReadActionConfinementLifetimeToken.allowOnEdt.get()) return action()
    KtReadActionConfinementLifetimeToken.allowOnEdt.set(true)
    try {
        return action()
    } finally {
        KtReadActionConfinementLifetimeToken.allowOnEdt.set(false)
    }
}

/**
 * Analysis is not supposed to be called from write action.
 * Such actions can lead to IDE freezes and incorrect behavior in some cases.
 *
 * There is no guarantee that PSI changes will be reflected in an Analysis API world inside
 * one [analyze] session.
 * Example:
 * ```
 * // code to be analyzed
 * fun foo(): Int = 0
 *
 * // use case code
 * fun useCase() {
 *   analyse(function) {
 *    // 'getConstantFromExpressionBody' is an imaginary function
 *    val valueBefore = function.getConstantFromExpressionBody() // valueBefore is 0
 *
 *    changeExpressionBodyTo(1) // now function will looks like `fun foo(): Int = 1`
 *    val valueAfter = function.getConstantFromExpressionBody() // Wrong way: valueAfter is not guarantied to be '1'
 *   }
 *
 *   analyse(function) {
 *    val valueAfter = function.getConstantFromExpressionBody() // OK: valueAfter is guarantied to be '1'
 *   }
 * }
 * ```
 *
 * @see KtAnalysisSession
 * @see KtReadActionConfinementLifetimeToken
 */
@KtAllowAnalysisFromWriteAction
@KtAllowProhibitedAnalyzeFromWriteAction
public inline fun <T> allowAnalysisFromWriteAction(action: () -> T): T {
    if (KtReadActionConfinementLifetimeToken.allowFromWriteAction.get()) return action()
    KtReadActionConfinementLifetimeToken.allowFromWriteAction.set(true)
    try {
        return action()
    } finally {
        KtReadActionConfinementLifetimeToken.allowFromWriteAction.set(false)
    }
}