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

import a.<!INVISIBLE_REFERENCE("A", "private", "file")!>A<!>
import a.<!INVISIBLE_REFERENCE("foo", "private", "file")!>foo<!>
import a.makeA
import a.<!INVISIBLE_REFERENCE("PO", "private", "file")!>PO<!>

fun test() {
    val y = makeA()
    y.<!INVISIBLE_MEMBER("A", "private", "file")!>bar<!>()
    <!INVISIBLE_MEMBER("foo", "private", "file")!>foo<!>()

    val u : <!INVISIBLE_REFERENCE("A", "private", "file")!>A<!> = <!INVISIBLE_MEMBER("A", "private", "file")!>A<!>()
    val a : java.util.Arrays.<!INVISIBLE_REFERENCE("ArrayList", "private", "'Arrays'")!>ArrayList<!><Int>;

    val po = <!INVISIBLE_MEMBER("PO", "private", "file")!>PO<!>
}

class B : <!EXPOSED_SUPER_CLASS!><!INVISIBLE_MEMBER("A", "private", "file"), INVISIBLE_REFERENCE("A", "private", "file")!>A<!>()<!> {}

class Q {
    class W {
        fun foo() {
            val y = makeA() //assure that 'makeA' is visible
        }
    }
}

//check that 'toString' can be invoked without specifying return type
class NewClass : java.util.ArrayList<<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!>>() {
    public override fun toString() = "a"
}
