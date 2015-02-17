package d

//import from objects before properties resolve

import d.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>A<!>.*
import d.M.R
import d.M.R.<!CANNOT_BE_IMPORTED!>bar<!>
import d.M.T
import d.M.Y

var r: T = T()
val y: T = Y

fun f() {
    <!UNRESOLVED_REFERENCE!>bar<!>()
    R.bar()
    <!UNRESOLVED_REFERENCE!>B<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
}

object M {
    object R {
        fun bar() {}
    }
    open class T() {}

    object Y : T() {}
}

object A {
    object B {
        fun foo() {}
    }
}