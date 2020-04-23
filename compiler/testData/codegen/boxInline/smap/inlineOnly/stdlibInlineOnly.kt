// FILE: 1.kt
package test

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T, R> T.myLet(block: (T) -> R) = block(this)

// FILE: 2.kt
import test.*

fun box(): String {
    // should not have any line numbers
    val k = "".myLet {
        it + "K"
    }
    return "O".myLet(fun (it: String): String {
        return it + k
    })
}

// FILE: 2.smap
