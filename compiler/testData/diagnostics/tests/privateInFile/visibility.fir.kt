// !DIAGNOSTICS: -UNUSED_VARIABLE

//FILE:file1.kt
package a

private open class A {
    fun bar() {}
}

private var x: Int = 10

var xx: Int = 20
  private set(value: Int) {}

private fun foo() {}

private fun bar() {
    val y = x
    x = 20
    xx = 30
}

fun makeA() = A()

private object PO {}

//FILE:file2.kt
package a

fun test() {
    val y = makeA()
    y.bar()
    <!INAPPLICABLE_CANDIDATE!>foo<!>()

    val u : A = <!INAPPLICABLE_CANDIDATE!>A<!>()

    val z = <!INAPPLICABLE_CANDIDATE!>x<!>
    <!INAPPLICABLE_CANDIDATE!>x<!> = 30

    val po = <!INAPPLICABLE_CANDIDATE!>PO<!>

    val v = xx
    xx = 40
}

class B : <!INAPPLICABLE_CANDIDATE!>A<!>() {}

class Q {
    class W {
        fun foo() {
            val y = makeA() //assure that 'makeA' is visible
        }
    }
}
