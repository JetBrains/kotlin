// IGNORE_INLINER_K2: IR
// SEPARATE_SMAP_DUMPS
// SKIP_INLINE_CHECK_IN: lParams$default

// FILE: 1.kt

package test
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]

inline fun kValue(crossinline s: () -> String): String { val lambda = { s() + "K" }; return lambda() }

inline fun lParams(initParams: () -> String = {
    val lambda = { "" + kValue { "O" } }; lambda()
}): String {
    val z = "body"
    return initParams()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return lParams()
}
