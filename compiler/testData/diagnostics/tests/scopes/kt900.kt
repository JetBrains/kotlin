//FILE:a.kt
//KT-900 Inaccessible class should be unresolved

package a

fun foo() {
    val <!UNUSED_VARIABLE!>b<!> : <!UNRESOLVED_REFERENCE!>B<!> = <!UNRESOLVED_REFERENCE!>B<!>() //only B() is unresolved, but in ": B" and "B.foo()" B should also be unresolved
    <!UNRESOLVED_REFERENCE!>B<!>.foo()

    <!UNRESOLVED_REFERENCE!>P<!>.foo()

    <!UNRESOLVED_REFERENCE!>M<!>.bar()
}

class A() {
    class object {
        class B() {
            class object {
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

import b.<!CANNOT_IMPORT_FROM_ELEMENT!>N<!>.M
import b.A.P
import b.A.B

fun foo() {
    val <!UNUSED_VARIABLE!>b<!> : B = B()
    B.foo()

    P.foo()

    <!UNRESOLVED_REFERENCE!>M<!>.bar()
}

class A() {
    class object {
        class B() {
            class object {
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