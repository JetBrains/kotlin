// LANGUAGE: +MultiPlatformProjects

// MODULE: lib
// FILE: lib.kt

package foo

fun transform(x: String, f: (String) -> String): String {
    return f(x) + "K"
}

// MODULE: platform()()(lib)
// FILE: platform.kt

package bar

import foo.*

fun box() = transform("") { "O" }
