// FILE: B.kt

package b.d

expect interface Other

expect class Another

fun baz() {}

// FILE: A.kt

package a.d

import b.d.*

fun foo(arg: Other): Another

fun bar() {
    baz()
}
