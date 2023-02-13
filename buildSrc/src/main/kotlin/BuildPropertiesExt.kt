/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val KotlinBuildProperties.includeJava9: Boolean
    get() = !isInJpsBuildIdeaSync && getBoolean("kotlin.build.java9", true)

val KotlinBuildProperties.useBootstrapStdlib: Boolean
    get() = isInJpsBuildIdeaSync || getBoolean("kotlin.build.useBootstrapStdlib", false)

val KotlinBuildProperties.postProcessing: Boolean get() = isTeamcityBuild || getBoolean("kotlin.build.postprocessing", true)

val KotlinBuildProperties.relocation: Boolean get() = postProcessing

val KotlinBuildProperties.proguard: Boolean get() = postProcessing && getBoolean("kotlin.build.proguard", isTeamcityBuild)

val KotlinBuildProperties.jarCompression: Boolean get() = getBoolean("kotlin.build.jar.compression", isTeamcityBuild)

val KotlinBuildProperties.ignoreTestFailures: Boolean get() = getBoolean("ignoreTestFailures", isTeamcityBuild)

val KotlinBuildProperties.disableWerror: Boolean
    get() = getBoolean("kotlin.build.disable.werror") || useFir || isInJpsBuildIdeaSync || getBoolean("test.progressive.mode")

val KotlinBuildProperties.generateModularizedConfigurations: Boolean
    get() = getBoolean("kotlin.fir.modularized.mt.configurations", false)

val KotlinBuildProperties.generateFullPipelineConfigurations: Boolean
    get() = getBoolean("kotlin.fir.modularized.fp.configurations", false)

val KotlinBuildProperties.pathToKotlinModularizedTestData: String?
    get() = getOrNull("kotlin.fir.modularized.testdata.kotlin") as? String

val KotlinBuildProperties.pathToIntellijModularizedTestData: String?
    get() = getOrNull("kotlin.fir.modularized.testdata.intellij") as? String

val KotlinBuildProperties.pathToYoutrackModularizedTestData: String?
    get() = getOrNull("kotlin.fir.modularized.testdata.youtrack") as? String

val KotlinBuildProperties.pathToSpaceModularizedTestData: String?
    get() = getOrNull("kotlin.fir.modularized.testdata.space") as? String

val KotlinBuildProperties.isObsoleteJdkOverrideEnabled: Boolean
    get() = getBoolean("kotlin.build.isObsoleteJdkOverrideEnabled", false)

val KotlinBuildProperties.isNativeRuntimeDebugInfoEnabled: Boolean
    get() = getBoolean("kotlin.native.isNativeRuntimeDebugInfoEnabled", false)

val KotlinBuildProperties.junit5NumberOfThreadsForParallelExecution: Int?
    get() = (getOrNull("kotlin.test.junit5.maxParallelForks") as? String)?.toInt()

// Enabling publishing docs jars only on CI build by default
// Currently dokka task runs non-incrementally and takes big amount of time
val KotlinBuildProperties.publishGradlePluginsJavadoc: Boolean
    get() = getBoolean("kotlin.build.gradle.publish.javadocs", isTeamcityBuild)

val KotlinBuildProperties.useFirWithLightTree: Boolean
    get() = getBoolean("kotlin.build.useFirLT")

val KotlinBuildProperties.useFirTightIC: Boolean
    get() = getBoolean("kotlin.build.useFirIC")
