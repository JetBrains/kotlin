// FILE: A.kt

package a

fun foo() = "OK"

// FILE: B.kt

package b

import a.foo

fun box() = foo()

