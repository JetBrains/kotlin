// FUNCTION: foo

package test.classes

class Outer {
    inner class Inner {
        fun bar()

        fun foo() {
            val outer = Outer()
            val inner = outer.Inner()
            inner.bar()
        }
    }
}