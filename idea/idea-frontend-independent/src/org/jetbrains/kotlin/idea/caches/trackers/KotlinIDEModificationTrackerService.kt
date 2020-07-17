/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.trackers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.psi.KtFile

class KotlinIDEModificationTrackerService(project: Project) : KotlinModificationTrackerService() {
    override val modificationTracker: ModificationTracker = PsiModificationTracker.SERVICE.getInstance(project)

    override val outOfBlockModificationTracker: ModificationTracker =
        KotlinCodeBlockModificationListener.getInstance(project).kotlinOutOfCodeBlockTracker

    override fun fileModificationTracker(file: KtFile): ModificationTracker =
        file.perFileModificationTracker
}