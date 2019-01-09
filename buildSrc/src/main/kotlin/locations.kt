/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project

val Project.ideaPluginDir get() = p( /* Idea plugin dir enforced by Big Kotlin */"ideaPluginDir") {
    // otherwise use some standard location
    "${ultimateProject(":prepare-deps:idea-plugin").buildDir}/external-deps/ideaPlugin/Kotlin"
}

val Project.appcodePluginDir get() = "$artifactsDir/appcodePlugin/Kotlin"
val Project.clionPluginDir get() = "$artifactsDir/clionPlugin/Kotlin"

val Project.clionDir get() = "${ultimateProject(":prepare-deps:cidr").buildDir}/external-deps/clion"

internal val Project.artifactsDir get() = "${rootProject.rootDir}/dist/artifacts"
