// !WITH_NEW_INFERENCE
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
    <!CANNOT_COMPLETE_RESOLVE{OI}, OVERLOAD_RESOLUTION_AMBIGUITY{NI}!>f<!>(l)
}
