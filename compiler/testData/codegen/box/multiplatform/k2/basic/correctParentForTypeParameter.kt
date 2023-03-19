// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects
// MODULE: lib
// FILE: lib.kt

package foo

fun transform(x: String, f: (String) -> String): String {
    return f(x) + "K"
}

// MODULE: lib2()()(lib)
// TARGET_BACKEND: JVM_IR
// FILE: main.kt

package bar

import foo.*

fun box() = transform("") { "O" }