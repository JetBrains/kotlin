// PROPERTY: foo

package test.classes

class Outer {
    inner class Inner {
        fun bar(): Int

        val foo: Int
            get() {
                val outer = Outer()
                val inner = outer.Inner()
                return inner.bar()
            }
    }
}