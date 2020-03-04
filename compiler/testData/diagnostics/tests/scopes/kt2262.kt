// FIR_IDENTICAL
package kt2262

//KT-2262 Cannot access protected member from inner class of subclass

abstract class Foo {
    protected val color: String = "red"
}

class Bar : Foo() {
    protected val i: Int = 1

    inner class Baz {
        val copy = color // INVISIBLE_MEMBER: Cannot access 'color' in 'Bar'
        val j = i
    }
}