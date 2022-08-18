// TARGET_BACKEND: NATIVE
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
package lib

lateinit var bar: String

// MODULE: main(lib)
// FILE: box.kt
import lib.*

fun box(): String {
    if (::bar.isInitialized) return "Fail 1"
    bar = "OK"
    if (!::bar.isInitialized) return "Fail 2"
    return bar
}
