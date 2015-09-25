// import all members from companion object
package c

import c.A.Companion.B
import c.<!CANNOT_IMPORT_MEMBERS_FROM_SINGLETON!>M<!>.*

fun foo() {
    val <!UNUSED_VARIABLE!>b<!>: B = B()
    var <!UNUSED_VARIABLE!>r<!>: R = R()
}

class A() {
    companion object {
        class B() {
            companion object {
            }
        }
    }
}

object M {
    fun foo() {}
    class R() {}
}