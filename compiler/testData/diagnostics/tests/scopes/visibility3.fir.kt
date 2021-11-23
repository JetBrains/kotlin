// !DIAGNOSTICS: -UNUSED_VARIABLE

//FILE:file1.kt
package a

private open class A {
    fun bar() {}
}

private var x: Int = 10

private fun foo() {}

private fun bar() {
    val y = x
    x = 20
}

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>makeA<!>() = A()

private object PO {}

//FILE:file2.kt
package a

fun test() {
    val y = makeA()
    y.<!INVISIBLE_REFERENCE!>bar<!>()
    <!INVISIBLE_REFERENCE!>foo<!>()

    val u : <!INVISIBLE_REFERENCE!>A<!> = <!INVISIBLE_REFERENCE!>A<!>()

    val z = <!INVISIBLE_REFERENCE!>x<!>
    <!INVISIBLE_REFERENCE!>x<!> = 30

    val po = <!INVISIBLE_REFERENCE!>PO<!>
}

class B : <!EXPOSED_SUPER_CLASS, INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>A<!>() {}

class Q {
    class W {
        fun foo() {
            val y = makeA() //assure that 'makeA' is visible
        }
    }
}
