// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// WITH_STDLIB
// A leading `<` in a package name segment works across modules, unlike a leading `<` in a declaration name.

// MODULE: lib
// FILE: lib.kt
package lib.`<p`

class X {
    fun value(): String = "class"
}

fun topLevel(): String = "function"

// MODULE: main(lib)
// FILE: main.kt
import lib.`<p`.X
import lib.`<p`.topLevel

fun box(): String {
    if (X().value() != "class") return "FAIL: class"
    if (topLevel() != "function") return "FAIL: function"
    return "OK"
}
