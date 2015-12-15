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
    X = 1
}
