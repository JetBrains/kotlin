// WITH_STDLIB
// FILE: 1.kt

package test

inline fun stub() {

}
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline val prop: String
    get() = "OK"

// FILE: 2.kt
import test.*

fun box(): String {
    return prop
}
