/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.location

/**
 * Describes where script files can be found
 */
@Deprecated("Experimental API")
enum class ScriptExpectedLocation {
    SourcesOnly, // Under sources roots
    TestsOnly,   // Under test sources roots
    Libraries,   // Under libraries classes or sources
    Project,     // Under project folder, including sources and test sources roots
    Everywhere;
}

@Deprecated("Experimental API")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptExpectedLocations(
    val value: Array<ScriptExpectedLocation> = [ScriptExpectedLocation.SourcesOnly, ScriptExpectedLocation.TestsOnly]
)