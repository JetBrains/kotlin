//FILE:a.kt
package a

private open class A {
    fun bar() {}
}

private fun foo() {}

fun makeA() = A()

private object PO {}

//FILE:b.kt
//+JDK
package b

import a.<!INVISIBLE_REFERENCE!>A<!>
import a.<!INVISIBLE_REFERENCE!>foo<!>
import a.makeA
import a.<!INVISIBLE_REFERENCE!>PO<!>

fun test() {
    val y = makeA()
    y.<!INVISIBLE_MEMBER!>bar<!>()
    <!INVISIBLE_MEMBER!>foo<!>()

    val <!UNUSED_VARIABLE!>u<!> : <!INVISIBLE_REFERENCE!>A<!> = <!INVISIBLE_MEMBER!>A<!>()
    val <!UNUSED_VARIABLE!>a<!> : java.util.Arrays.<!INVISIBLE_REFERENCE!>ArrayList<!><Int>;

    val <!UNUSED_VARIABLE!>po<!> = <!INVISIBLE_MEMBER!>PO<!>
}

class B : <!INVISIBLE_REFERENCE, INVISIBLE_MEMBER!>A<!>() {}

class Q {
    class W {
        fun foo() {
            val <!UNUSED_VARIABLE!>y<!> = makeA() //assure that 'makeA' is visible
        }
    }
}

//check that 'toString' can be invoked without specifying return type
class NewClass : java.util.ArrayList<Integer>() {
    public override fun toString() = "a"
}