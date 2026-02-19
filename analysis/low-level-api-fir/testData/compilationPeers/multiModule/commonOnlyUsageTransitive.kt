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


// MODULE: main(common, baseCommon, baseJvm)
// TARGET_PLATFORM: JVM

// FILE: main.kt
package main
import test.*

fun test() {
    lib()
}