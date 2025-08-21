// MODULE: lib
// LANGUAGE: +MultiPlatformProjects

// TARGET_PLATFORM: Common
// FILE: lib.kt

package lib

inline fun lib(): String = "foo"

// MODULE: main(lib)
// TARGET_PLATFORM: JVM
// FILE: main.kt

import lib.lib

fun test() {
    <caret>lib()
}