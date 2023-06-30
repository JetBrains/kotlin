/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.USE_STATE_KEEPER
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@OptIn(PrivateSessionConstructor::class)
abstract class LLFirSession(
    val ktModule: KtModule,
    override val builtinTypes: BuiltinTypes,
    kind: Kind
) : FirSession(sessionProvider = null, kind) {
    abstract fun getScopeSession(): ScopeSession

    val project: Project
        get() = ktModule.project

    private val _isValid = AtomicBoolean(true)

    /**
     * Whether the [LLFirSession] is valid. The session should not be used if it is invalid.
     *
     * [isValid] should be set to `false` at the same time as the session is removed from [LLFirSessionCache]. Hence, [isValid] should be
     * managed by [LLFirSessionCache].
     */
    var isValid: Boolean
        get() = _isValid.get()
        internal set(value) {
            check(!value) { "An invalid LL FIR session cannot become valid again." }
            _isValid.set(value)
        }

    fun invalidate() {
        val application = ApplicationManager.getApplication()
        if (application.isWriteAccessAllowed) {
            invalidateInWriteAction()
        } else {
            // We have to invalidate the session on the EDT per the contract of `LLFirSessionInvalidationService`. The timing here is not
            // 100% waterproof, but `LLFirSession.invalidate` is only a workaround for when FIR guards consistency protection (see KT-56503)
            // is turned off. The check restricts usage of `invalidate` to this scenario.
            check(!USE_STATE_KEEPER) {
                "Outside a write action, a session may only be invalidated directly when FIR guards are turned off."
            }

            application.invokeLater(
                { application.runWriteAction { invalidateInWriteAction() } },

                // `ModalityState.any()` can be used because session invalidation does not modify PSI, VFS, or the project model.
                ModalityState.any(),
            )
        }
    }

    private fun invalidateInWriteAction() {
        if (!isValid) return

        LLFirSessionInvalidationService.getInstance(project).invalidate(ktModule)
    }

    /**
     * Creates a [ModificationTracker] which tracks the validity of this session via [isValid].
     */
    fun createValidityTracker(): ModificationTracker = ValidityModificationTracker()

    private inner class ValidityModificationTracker : ModificationTracker {
        private var count = AtomicLong()

        override fun getModificationCount(): Long {
            if (isValid) return 0

            // When the session is invalid, we cannot simply return a static modification count of 1. For example, consider situations where
            // a cached value was created with an already invalid session (so it remembers the modification count of 1). Then, if we return
            // a static modification count of 1, the modification count never changes and the cached value misses that the session has been
            // invalidated. Hence, `count` is incremented on each modification count access.
            return count.incrementAndGet()
        }
    }
}

abstract class LLFirModuleSession(
    ktModule: KtModule,
    builtinTypes: BuiltinTypes,
    kind: Kind
) : LLFirSession(ktModule, builtinTypes, kind)

val FirElementWithResolveState.llFirSession: LLFirSession
    get() = moduleData.session as LLFirSession

val FirBasedSymbol<*>.llFirSession: LLFirSession
    get() = moduleData.session as LLFirSession