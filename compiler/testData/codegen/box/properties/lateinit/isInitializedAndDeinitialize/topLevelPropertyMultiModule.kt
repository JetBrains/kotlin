// TARGET_BACKEND: NATIVE
// In K2, isInitialized on a property from another file is forbidden for all backends.
// IGNORE_BACKEND_K2: NATIVE
// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
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
