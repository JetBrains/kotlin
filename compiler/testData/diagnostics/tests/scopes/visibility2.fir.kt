// !DIAGNOSTICS: -UNUSED_VARIABLE
// JAVAC_EXPECTED_FILE
//FILE:a.kt
package a

private open class A {
    fun bar() {}
}

private fun foo() {}

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>makeA<!>() = A()

private object PO {}

//FILE:b.kt
//+JDK
package b

import a.A
import a.foo
import a.makeA
import a.PO

fun test() {
    val y = makeA()
    y.bar()
    <!HIDDEN!>foo<!>()

    val u : A = <!HIDDEN!>A<!>()
    val a : java.util.Arrays.ArrayList<Int>;

    val po = <!HIDDEN!>PO<!>
}

class B : <!EXPOSED_SUPER_CLASS, HIDDEN!>A<!>() {}

class Q {
    class W {
        fun foo() {
            val y = makeA() //assure that 'makeA' is visible
        }
    }
}

//check that 'toString' can be invoked without specifying return type
class NewClass : java.util.ArrayList<Integer>() {
    public override fun toString() = "a"
}
