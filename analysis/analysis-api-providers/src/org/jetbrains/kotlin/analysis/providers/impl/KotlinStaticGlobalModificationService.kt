/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly

public class KotlinStaticGlobalModificationService(private val project: Project) : KotlinGlobalModificationServiceBase(project) {
    @TestOnly
    override fun incrementModificationTrackers(includeBinaryTrackers: Boolean) {
        KotlinStaticModificationTrackerFactory.getInstance(project).incrementModificationsCount(includeBinaryTrackers)
    }
}
