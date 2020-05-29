/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.idea.caches.trackers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import org.jetbrains.kotlin.idea.KotlinLanguage

/**
 * Tested in OutOfBlockModificationTestGenerated
 */
// FIX ME WHEN BUNCH 193 REMOVED
class KotlinCodeBlockModificationListener(
    project: Project,
    treeAspect: TreeAspect
) : KotlinCodeBlockModificationListenerCompat(project) {

    @Suppress("UnstableApiUsage")
    private val isLanguageTrackerEnabled = modificationTrackerImpl.isEnableLanguageTrackerCompat

    // FIX ME WHEN BUNCH 191 REMOVED
    // When there're we no per-language trackers we had to increment global tracker first and process result afterward
    private val customIncrement = if (isLanguageTrackerEnabled) 0 else 1

    init {
        init(
            treeAspect,
            incOCBCounter = { ktFile ->
                if (isLanguageTrackerEnabled) {
                    kotlinOutOfCodeBlockTrackerImpl.incModificationCount()
                    perModuleOutOfCodeBlockTrackerUpdater.onKotlinPhysicalFileOutOfBlockChange(ktFile, true)
                } else {
                    perModuleOutOfCodeBlockTrackerUpdater.onKotlinPhysicalFileOutOfBlockChange(ktFile, false)
                    // Increment counter and process changes in PsiModificationTracker.Listener
                    modificationTrackerImpl.incCounter()
                }
            },
            kotlinOutOfCodeBlockTrackerProducer = {
                if (isLanguageTrackerEnabled) {
                    SimpleModificationTracker()
                } else {
                    object : SimpleModificationTracker() {
                        override fun getModificationCount(): Long {
                            @Suppress("DEPRECATION")
                            return modificationTrackerImpl.outOfCodeBlockModificationCount
                        }
                    }
                }
            },
            psiModificationTrackerListener = {
                @Suppress("UnstableApiUsage")
                if (isLanguageTrackerEnabled) {
                    val kotlinTrackerInternalIDECount =
                        modificationTrackerImpl.forLanguage(KotlinLanguage.INSTANCE).modificationCount
                    if (kotlinModificationTracker == kotlinTrackerInternalIDECount) {
                        // Some update that we are not sure is from Kotlin language, as Kotlin language tracker wasn't changed
                        kotlinOutOfCodeBlockTrackerImpl.incModificationCount()
                    } else {
                        kotlinModificationTracker = kotlinTrackerInternalIDECount
                    }
                }

                perModuleOutOfCodeBlockTrackerUpdater.onPsiModificationTrackerUpdate(customIncrement)
            }
        )
    }

    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        assert(isLanguageTrackerEnabled)
        super.treeChanged(event)
    }

    companion object {
        fun getInstance(project: Project): KotlinCodeBlockModificationListener =
            project.getComponent(KotlinCodeBlockModificationListener::class.java)
    }
}
