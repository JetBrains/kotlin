// FILE: foo.kt
package foo

fun f<T>(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

// FILE: main.kt

import foo.*

fun f<T>(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

fun test<T>(l: List<T>) {
    <!CANNOT_COMPLETE_RESOLVE!>f<!>(l)
}