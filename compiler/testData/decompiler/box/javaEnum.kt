// FILE: A.kt

package a

fun foo() {}

// FILE: B.kt

package b

import a.foo

fun test() {
    foo()
}
