/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.BooleanModificationTracker
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.utils.trackers.CompositeModificationTracker
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirElementWithResolvePhase
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(PrivateSessionConstructor::class)
abstract class LLFirSession(
    val ktModule: KtModule,
    override val builtinTypes: BuiltinTypes,
    kind: Kind
) : FirSession(sessionProvider = null, kind) {
    abstract fun getScopeSession(): ScopeSession

    val modificationTracker: ModificationTracker

    private val initialModificationCount: Long
    private val isExplicitlyInvalidated = AtomicBoolean(false)

    val project: Project
        get() = ktModule.project

    init {
        val trackerFactory = KotlinModificationTrackerFactory.getService(ktModule.project)
        val validityTracker = trackerFactory.createModuleStateTracker(ktModule)

        val outOfBlockTracker = when (ktModule) {
            is KtSourceModule -> trackerFactory.createModuleWithoutDependenciesOutOfBlockModificationTracker(ktModule)
            is KtNotUnderContentRootModule -> ModificationTracker { ktModule.file?.modificationStamp ?: 0 }
            is KtScriptModule -> ModificationTracker { ktModule.file.modificationStamp }
            is KtScriptDependencyModule -> ModificationTracker { ktModule.file?.modificationStamp ?: 0 }
            else -> ModificationTracker.NEVER_CHANGED
        }

        modificationTracker = CompositeModificationTracker.create(
            listOf(
                outOfBlockTracker,
                ModificationTracker { validityTracker.rootModificationCount },
                BooleanModificationTracker { validityTracker.isValid },
                BooleanModificationTracker { !isExplicitlyInvalidated.get() }
            )
        )

        initialModificationCount = modificationTracker.modificationCount
    }

    fun invalidate() {
        isExplicitlyInvalidated.set(true)
    }

    val isValid: Boolean
        get() = modificationTracker.modificationCount == initialModificationCount
}

abstract class LLFirModuleSession(
    ktModule: KtModule,
    builtinTypes: BuiltinTypes,
    kind: Kind
) : LLFirSession(ktModule, builtinTypes, kind)

val FirElementWithResolvePhase.llFirSession: LLFirSession
    get() = moduleData.session as LLFirSession

val FirBasedSymbol<*>.llFirSession: LLFirSession
    get() = moduleData.session as LLFirSession