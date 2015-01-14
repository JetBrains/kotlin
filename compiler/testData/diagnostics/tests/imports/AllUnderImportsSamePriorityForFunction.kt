// FILE: a.kt
package a

fun X(<!UNUSED_PARAMETER!>p<!>: Int) {}

// FILE: b.kt
package b

fun X(): Int = 1

// FILE: c.kt
package c

import b.*
import a.X

fun foo() {
    val <!UNUSED_VARIABLE!>v<!>: Int = X()
}
