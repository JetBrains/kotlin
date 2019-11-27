// FILE: A.kt

package foo

class A {
    fun foo() {}
}

// FILE: main.kt

import foo.A as B

fun test_1() {
    val a = <!UNRESOLVED_REFERENCE!>A<!>()
    val b = <!UNRESOLVED_REFERENCE!>B<!>() // should be OK
    val c: B = <!UNRESOLVED_REFERENCE!>A<!>()
}

fun test_2(b: B) {
    b.foo()
}