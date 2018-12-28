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
    y.<!INVISIBLE_MEMBER("A", "private", "file")!>bar<!>()
    <!INVISIBLE_MEMBER("foo", "private", "file")!>foo<!>()

    val u : <!INVISIBLE_REFERENCE("A", "private", "file")!>A<!> = <!INVISIBLE_MEMBER("A", "private", "file")!>A<!>()

    val z = <!INVISIBLE_MEMBER("x", "private", "file")!>x<!>
    <!INVISIBLE_MEMBER("x", "private", "file")!>x<!> = 30

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
