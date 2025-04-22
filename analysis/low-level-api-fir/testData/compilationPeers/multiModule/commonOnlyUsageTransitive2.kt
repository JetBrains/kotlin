// DUMP_IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: baseCommon
// TARGET_PLATFORM: Common

// FILE: BaseCommon.kt
package base

expect inline fun base(): String

// FILE: BaseCommon2.kt
package base

inline fun root(): String = "root"

// FILE: BaseUnneeded.kt
package base

inline fun root(text: String): String = text


// MODULE: baseJvm()()(baseCommon)
// TARGET_PLATFORM: JVM

// FILE: BaseJvm.kt
package base

actual inline fun base(): String = root()


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