// import all members from default object
package c

import c.A.Default.B
import c.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>M<!>.*

fun foo() {
    val <!UNUSED_VARIABLE!>b<!>: B = B()
    var <!UNUSED_VARIABLE!>r<!>: <!UNRESOLVED_REFERENCE!>R<!> = <!UNRESOLVED_REFERENCE!>R<!>()
}

class A() {
    default object {
        class B() {
            default object {
            }
        }
    }
}

object M {
    fun foo() {}
    class R() {}
}