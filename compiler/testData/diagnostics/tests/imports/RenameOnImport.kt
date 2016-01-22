// FILE: a.kt
package a

val x = 1
val y = 1

// FILE: b.kt
package b

val x = ""

// FILE: c.kt
package c

import a.x as AX
import a.*
import b.*
import a.y as AY

val v1: Int = AX
val v2: String = x
val v3 = <!UNRESOLVED_REFERENCE!>y<!>
