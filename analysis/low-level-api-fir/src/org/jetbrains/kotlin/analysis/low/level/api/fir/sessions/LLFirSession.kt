/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KtModuleStateTracker
import org.jetbrains.kotlin.analysis.utils.trackers.CompositeModificationTracker
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(PrivateSessionConstructor::class)
abstract class LLFirSession(
    val ktModule: KtModule,
    dependencyTracker: ModificationTracker,
    override val builtinTypes: BuiltinTypes,
    kind: Kind
) : FirSession(sessionProvider = null, kind) {
    abstract fun getScopeSession(): ScopeSession

    private val initialModificationCount: Long
    private val isExplicitlyInvalidated = AtomicBoolean(false)

    val modificationTracker: ModificationTracker

    val project: Project
        get() = ktModule.project

    init {
        val trackerFactory = KotlinModificationTrackerFactory.getService(ktModule.project)
        val validityTracker = trackerFactory.createModuleStateTracker(ktModule)

        val outOfBlockTracker = when (ktModule) {
            is KtSourceModule -> trackerFactory.createModuleWithoutDependenciesOutOfBlockModificationTracker(ktModule)
            is KtNotUnderContentRootModule -> ktModule.file?.let(::FileModificationTracker)
            is KtScriptModule -> FileModificationTracker(ktModule.file)
            is KtScriptDependencyModule -> ktModule.file?.let(::FileModificationTracker)
            else -> null
        }

        modificationTracker = CompositeModificationTracker.createFlattened(
            listOfNotNull(
                ExplicitInvalidationTracker(ktModule, isExplicitlyInvalidated),
                ModuleStateModificationTracker(ktModule, validityTracker),
                outOfBlockTracker,
                dependencyTracker
            )
        )

        initialModificationCount = modificationTracker.modificationCount
    }

    private class ModuleStateModificationTracker(val module: KtModule, val tracker: KtModuleStateTracker) : ModificationTracker {
        override fun getModificationCount(): Long = tracker.rootModificationCount
        override fun toString(): String = "Module state tracker for module '${module.moduleDescription}'"
    }

    private class ExplicitInvalidationTracker(val module: KtModule, val isExplicitlyInvalidated: AtomicBoolean) : ModificationTracker {
        override fun getModificationCount(): Long = if (isExplicitlyInvalidated.get()) 1 else 0
        override fun toString(): String = "Explicit invalidation tracker for module '${module.moduleDescription}'"
    }

    private class FileModificationTracker(file: PsiFile) : ModificationTracker {
        private val pointer = SmartPointerManager.getInstance(file.project).createSmartPsiElementPointer(file)

        override fun getModificationCount(): Long {
            val file = pointer.element ?: return Long.MAX_VALUE
            return file.modificationStamp
        }

        override fun toString(): String {
            val file = pointer.element ?: return "File tracker for a collected file"
            val virtualFile = file.virtualFile ?: return "File tracker for a non-physical file '${file.name}'"
            return "File tracker for path '${virtualFile.path}'"
        }
    }

    fun invalidate() {
        isExplicitlyInvalidated.set(true)
    }

    val isValid: Boolean
        get() = modificationTracker.modificationCount == initialModificationCount
}

abstract class LLFirModuleSession(
    ktModule: KtModule,
    dependencyTracker: ModificationTracker,
    builtinTypes: BuiltinTypes,
    kind: Kind
) : LLFirSession(ktModule, dependencyTracker, builtinTypes, kind)

val FirElementWithResolveState.llFirSession: LLFirSession
    get() = moduleData.session as LLFirSession

val FirBasedSymbol<*>.llFirSession: LLFirSession
    get() = moduleData.session as LLFirSession