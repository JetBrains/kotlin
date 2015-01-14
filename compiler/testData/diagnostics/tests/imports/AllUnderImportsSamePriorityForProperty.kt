// FILE: a.kt
package a

var X: Int = 1

// FILE: b.kt
package b

var X: String = ""

// FILE: c.kt
package c

import a.X
import b.*

fun foo() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>X<!> = 1
}
