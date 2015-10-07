// FILE: foo.kt
package foo

fun <T> f(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

// FILE: main.kt

import foo.*

fun <T> f(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

fun <T> test(l: List<T>) {
    <!CANNOT_COMPLETE_RESOLVE!>f<!>(l)
}