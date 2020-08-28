/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.idea.caches.trackers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.idea.KotlinLanguage

/**
 * Tested in OutOfBlockModificationTestGenerated
 */
// FIX ME WHEN BUNCH 193 REMOVED
class KotlinCodeBlockModificationListener(
    project: Project,
    treeAspect: TreeAspect
) : KotlinCodeBlockModificationListenerCompat(project) {

    init {
        init(
            treeAspect,
            incOCBCounter = { ktFile ->
                kotlinOutOfCodeBlockTrackerImpl.incModificationCount()
                perModuleOutOfCodeBlockTrackerUpdater.onKotlinPhysicalFileOutOfBlockChange(ktFile, true)
            },
            kotlinOutOfCodeBlockTrackerProducer = {
                SimpleModificationTracker()
            },
            psiModificationTrackerListener = {
                @Suppress("UnstableApiUsage")
                val kotlinTrackerInternalIDECount =
                    modificationTrackerImpl.forLanguage(KotlinLanguage.INSTANCE).modificationCount
                if (kotlinModificationTracker == kotlinTrackerInternalIDECount) {
                    // Some update that we are not sure is from Kotlin language, as Kotlin language tracker wasn't changed
                    kotlinOutOfCodeBlockTrackerImpl.incModificationCount()
                } else {
                    kotlinModificationTracker = kotlinTrackerInternalIDECount
                }

                perModuleOutOfCodeBlockTrackerUpdater.onPsiModificationTrackerUpdate()
            }
        )
    }

    companion object {
        fun getInstance(project: Project): KotlinCodeBlockModificationListener =
            project.getComponent(KotlinCodeBlockModificationListener::class.java)
    }
}
