// DUMP_IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: lib
// TARGET_PLATFORM: Common
// FILE: lib.kt

package lib

fun lib(): String = "foo"

// MODULE: main(lib)
// TARGET_PLATFORM: JVM
// FILE: main.kt

import lib.lib

fun test() {
    <caret>lib()
}