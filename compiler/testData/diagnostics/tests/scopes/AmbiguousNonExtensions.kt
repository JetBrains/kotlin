// FIR_IDENTICAL
// FILE: a.kt
package a

val v = 1
fun f() = 1

// FILE: b.kt
package b

val v = 1
fun f() = 1

// FILE: main.kt
import a.*
import b.*

val vv = <!OVERLOAD_RESOLUTION_AMBIGUITY!>v<!>
val ff = <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
