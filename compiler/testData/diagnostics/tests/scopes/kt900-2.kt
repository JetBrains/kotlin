package d

//import from objects before properties resolve

import d.<!CANNOT_IMPORT_MEMBERS_FROM_SINGLETON!>A<!>.*
import d.M.R
import d.M.R.<!CANNOT_IMPORT_MEMBERS_FROM_SINGLETON!>bar<!>
import d.M.T
import d.M.Y

var r: T = T()
val y: T = Y

fun f() {
    <!UNRESOLVED_REFERENCE!>bar<!>()
    R.bar()
    B.foo()
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