//FILE:a.kt
//KT-900 Inaccessible class should be unresolved

package a

fun foo() {
    val <!UNUSED_VARIABLE!>b<!> : <!UNRESOLVED_REFERENCE!>B<!> = <!UNRESOLVED_REFERENCE!>B<!>() //only B() is unresolved, but in ": B" and "B.foo()" B should also be unresolved
    <!UNRESOLVED_REFERENCE!>B<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()

    <!UNRESOLVED_REFERENCE!>P<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()

    <!UNRESOLVED_REFERENCE!>M<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>()
}

class A() {
    companion object {
        class B() {
            companion object {
                fun foo() {}
            }
        }

        object P {
            fun foo() {}
        }
    }
}

object N {
    object M {
        fun bar() {}
    }
}

//FILE:b.kt
package b

import b.N.M
import b.A.Companion.P
import b.A.Companion.B

fun foo() {
    val <!UNUSED_VARIABLE!>b<!> : B = B()
    B.foo()

    P.foo()

    M.bar()
}

class A() {
    companion object {
        class B() {
            companion object {
                fun foo() {}
            }
        }

        object P {
            fun foo() {}
        }
    }
}

object N {
    object M {
        fun bar() {}
    }
}