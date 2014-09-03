// FILE: a.kt
package a

class A

// FILE: b.kt
package b

import a.A

val A.d: Int get() = 1

// FILE: c.kt
package c

import a.A

val A.d: Int get() = 2

// FILE: d.kt

package d

import a.A
import b.d
import c.d

fun A.test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>d<!>
}
