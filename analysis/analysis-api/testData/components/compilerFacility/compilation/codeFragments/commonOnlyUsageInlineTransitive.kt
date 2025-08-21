// DUMP_IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: baseCommon
// TARGET_PLATFORM: Common

// FILE: BaseCommon.kt
package base

expect inline fun base(): String


// MODULE: baseJvm()()(baseCommon)
// TARGET_PLATFORM: JVM

// FILE: BaseJvm.kt
package base

actual inline fun base(): String = "base"


// MODULE: common(baseCommon)
// TARGET_PLATFORM: Common

// FILE: Common.kt
package test
import base.*

inline fun lib(): String = base()


// MODULE: app(common, baseCommon, baseJvm)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt
package app
import test.*

fun test() {
    <caret_context>lib()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: app

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
lib()