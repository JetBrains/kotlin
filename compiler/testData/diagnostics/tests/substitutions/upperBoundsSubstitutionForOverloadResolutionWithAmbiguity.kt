// FIR_IDENTICAL
// FILE: foo.kt
package foo

fun <T> f(l: List<T>) {}

// FILE: bar.kt
package bar

fun <T> f(l: List<T>) {}

// FILE: main.kt

import foo.*
import bar.*

fun <T> test(l: List<T>) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>(l)
}
