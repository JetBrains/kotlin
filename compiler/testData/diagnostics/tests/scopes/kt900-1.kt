// import all members from class object
package c

import c.A.*
import c.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.*

fun foo() {
    val <!UNUSED_VARIABLE!>b<!>: B = B()
    var <!UNUSED_VARIABLE!>r<!>: <!UNRESOLVED_REFERENCE!>R<!> = <!UNRESOLVED_REFERENCE!>R<!>()
}

class A() {
    class object {
        class B() {
            class object {
            }
        }
    }
}

object M {
    fun foo() {}
    class R() {}
}