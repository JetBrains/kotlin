// MODULE: lib
// LANGUAGE: +MultiPlatformProjects

// TARGET_PLATFORM: Common
// FILE: lib.kt

package lib

inline val lib: String
    get() = "foo"


// MODULE: main(lib)
// TARGET_PLATFORM: JVM
// FILE: main.kt

import lib.lib

fun test() {
    <caret>lib
}