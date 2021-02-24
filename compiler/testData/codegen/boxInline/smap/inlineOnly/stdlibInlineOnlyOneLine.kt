// IGNORE_FIR_DIAGNOSTICS
// FILE: 1.kt
package test

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T, R> T.myLet(block: (T) -> R) = block(this)

// FILE: 2.kt
import test.*

fun box(): String {
    val k = "".myLet { it + "K" }
    return "O".myLet(fun (it: String): String { return it + k })
}

// See KT-23064 for the problem and InlineOnlySmapSkipper for an explanation.
