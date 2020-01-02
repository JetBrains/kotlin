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

val vv = <!AMBIGUITY!>v<!>
val ff = <!AMBIGUITY!>f<!>()
