// FILE: a.kt
package a

object O {
    class A
    object B

    fun bar() {}
}

// FILE: b.kt
package b

import a.<!CANNOT_IMPORT_MEMBERS_FROM_SINGLETON!>O<!>.*

fun test() {
    A()
    B
    <!UNRESOLVED_REFERENCE!>bar<!>()
}