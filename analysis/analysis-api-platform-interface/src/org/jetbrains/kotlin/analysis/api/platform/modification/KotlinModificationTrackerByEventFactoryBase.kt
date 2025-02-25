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
    protected val eventSourceModificationTracker: SimpleModificationTracker = SimpleModificationTracker()

    protected val eventLibraryModificationTracker: SimpleModificationTracker = SimpleModificationTracker()

    init {
        // It's generally best practice to register listeners in plugin XMLs. However, we don't know whether the platform intends to
        // implement this kind of modification tracker factory, and thus can't register these listeners in the XML.
        project.analysisMessageBus.connect(this).subscribe(
            KotlinModificationEvent.TOPIC,
            KotlinModificationEventListener { event ->
                when (event) {
                    is KotlinModuleStateModificationEvent,
                    KotlinGlobalModuleStateModificationEvent,
                    KotlinGlobalScriptModuleStateModificationEvent,
                        -> {
                        eventSourceModificationTracker.incModificationCount()
                        eventLibraryModificationTracker.incModificationCount()
                    }

                    is KotlinModuleOutOfBlockModificationEvent,
                    KotlinGlobalSourceModuleStateModificationEvent,
                    KotlinGlobalSourceOutOfBlockModificationEvent,
                        -> eventSourceModificationTracker.incModificationCount()

                    is KotlinCodeFragmentContextModificationEvent -> {}
                }
            }
        )
    }

    override fun createProjectWideSourceModificationTracker(): ModificationTracker = eventSourceModificationTracker

    override fun createProjectWideLibraryModificationTracker(): ModificationTracker = eventLibraryModificationTracker

    override fun dispose() {}
}
