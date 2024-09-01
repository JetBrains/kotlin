/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.modification

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationServiceBase

class KotlinStandaloneGlobalModificationService(private val project: Project) : KotlinGlobalModificationServiceBase(project) {
    @TestOnly
    override fun incrementModificationTrackers(includeBinaryTrackers: Boolean) {
        KotlinStandaloneModificationTrackerFactory.getInstance(project).incrementModificationsCount(includeBinaryTrackers)
    }
}
