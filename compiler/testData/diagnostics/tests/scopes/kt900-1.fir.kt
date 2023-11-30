// import all members from companion object
package c

import c.A.Companion.B
import c.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>M<!>.*

fun foo() {
    val b: B = B()
    var r: R = R()
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
