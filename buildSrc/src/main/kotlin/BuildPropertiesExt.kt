/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val KotlinBuildProperties.isCooperativeCompilationWithKotlinIde: Boolean
    get() = rootProjectDir.resolve("../kotlin.kotlin-ide.iml").exists()

val KotlinBuildProperties.includeJava9: Boolean
    get() = !isInIdeaSync && getBoolean("kotlin.build.java9", true)

val KotlinBuildProperties.postProcessing: Boolean get() = isTeamcityBuild || getBoolean("kotlin.build.postprocessing", true)

val KotlinBuildProperties.relocation: Boolean get() = postProcessing

val KotlinBuildProperties.proguard: Boolean get() = postProcessing && getBoolean("kotlin.build.proguard", isTeamcityBuild)

val KotlinBuildProperties.jarCompression: Boolean get() = getBoolean("kotlin.build.jar.compression", isTeamcityBuild)

val KotlinBuildProperties.ignoreTestFailures: Boolean get() = getBoolean("ignoreTestFailures", isTeamcityBuild)
