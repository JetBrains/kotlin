// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: a.kt

package a

enum class A { A1 }

fun <T : Enum<T>> foo(arg: T.() -> Unit) = 1

// FILE: b.kt

package b

fun <T : Any> foo(arg: T.() -> Unit) = 2

// FILE: test.kt

import a.*
import b.*

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!><A> { }
}
