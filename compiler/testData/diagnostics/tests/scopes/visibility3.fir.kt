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
    y.bar()
    <!HIDDEN!>foo<!>()

    val u : A = <!HIDDEN!>A<!>()

    val z = <!HIDDEN!>x<!>
    <!HIDDEN!>x<!> = 30

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
