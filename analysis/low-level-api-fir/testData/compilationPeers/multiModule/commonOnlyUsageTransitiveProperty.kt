// LANGUAGE: +MultiPlatformProjects

// MODULE: baseCommon
// TARGET_PLATFORM: Common

// FILE: BaseCommon.kt
package base

expect inline val base: String


// MODULE: baseJvm()()(baseCommon)
// TARGET_PLATFORM: JVM

// FILE: BaseJvm.kt
package base

actual inline val base: String
    get() = "base"


// MODULE: common(baseCommon)
// TARGET_PLATFORM: Common

// FILE: Common.kt
package test
import base.*

inline val lib: String
    get() = base


// MODULE: main(common, baseCommon, baseJvm)
// TARGET_PLATFORM: JVM

// FILE: main.kt
package main
import test.*

fun test() {
    lib
}