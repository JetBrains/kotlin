// FILE: A.kt

package foo

class A {
    fun foo() {}
}

// FILE: main.kt

import foo.A as B

fun test_1() {
    val a = <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>A<!>()<!>
    val b = B() // should be OK
    val c: B = <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>A<!>()<!>
}

fun test_2(b: B) {
    b.foo()
}
