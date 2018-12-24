/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

/**
 * Represents a single module (in IDEA sense) or a single project/subproject (in Gradle sense)
 * that can be built independently from others.
 */
class GradleKonanBuildModule(
    val id: String,
    val projectPath: String,
    moduleBuildTaskPath: String?,
    moduleCleanTaskPath: String?
) {
    val moduleBuildTaskPath: String? = moduleBuildTaskPath?.takeIf { it.isNotEmpty() }
    val moduleCleanTaskPath: String? = moduleCleanTaskPath?.takeIf { it.isNotEmpty() }
}
