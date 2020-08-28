/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.commonizer

import org.jetbrains.kotlin.gradle.EnableCommonizerTask
import org.jetbrains.plugins.gradle.model.ClassSetProjectImportModelProvider
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KotlinCommonizerModelResolver : AbstractProjectResolverExtension() {
    override fun requiresTaskRunning() = true

    override fun getProjectsLoadedModelProvider() = ClassSetProjectImportModelProvider(
        setOf(EnableCommonizerTask::class.java)
    )
}


