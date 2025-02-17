/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus

/**
 * A base implementation of [KotlinModificationTrackerFactory] which defines basic modification trackers that are incremented after
 * receiving [KotlinModificationEvent]s.
 */
public abstract class KotlinModificationTrackerByEventFactoryBase(project: Project) : KotlinModificationTrackerFactory, Disposable {
    protected val eventOutOfBlockModificationTracker: SimpleModificationTracker = SimpleModificationTracker()

    protected val eventLibrariesModificationTracker: SimpleModificationTracker = SimpleModificationTracker()

    init {
        // It's generally best practice to register listeners in plugin XMLs. However, we don't know whether the platform intends to
        // implement this kind of modification tracker factory, and thus can't register these listeners in the XML.
        project.analysisMessageBus.connect(this).subscribe(
            KotlinModificationEvent.TOPIC,
            KotlinModificationEventListener { event ->
                when (event) {
                    is KotlinModuleOutOfBlockModificationEvent,
                    KotlinGlobalSourceOutOfBlockModificationEvent
                        -> eventOutOfBlockModificationTracker.incModificationCount()

                    is KotlinModuleStateModificationEvent,
                    KotlinGlobalModuleStateModificationEvent,
                        -> eventLibrariesModificationTracker.incModificationCount()

                    is KotlinCodeFragmentContextModificationEvent,
                    KotlinGlobalScriptModuleStateModificationEvent,
                    KotlinGlobalSourceModuleStateModificationEvent
                        -> {
                    }
                }
            }
        )
    }

    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker = eventOutOfBlockModificationTracker

    override fun createLibrariesWideModificationTracker(): ModificationTracker = eventLibrariesModificationTracker

    override fun dispose() {}
}
