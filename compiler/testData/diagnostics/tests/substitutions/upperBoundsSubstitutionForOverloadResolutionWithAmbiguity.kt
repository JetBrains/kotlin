// !WITH_NEW_INFERENCE
// FILE: foo.kt
package foo

fun <T> f(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

// FILE: bar.kt
package bar

fun <T> f(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

// FILE: main.kt

import foo.*
import bar.*

fun <T> test(l: List<T>) {
    <!NI;OVERLOAD_RESOLUTION_AMBIGUITY, OI;CANNOT_COMPLETE_RESOLVE!>f<!>(l)
}