package bar

class Test {
    val foo: Int? = null
    fun foo(o: Test) = foo == null && o.foo == null // ERROR warning: o.test == null is always true

    fun bar(a: Test, b: Test) {
        if (a.foo != null) {
            useInt(<!ARGUMENT_TYPE_MISMATCH!>b.foo<!>)
        }
        if (a.foo != null) {
            useInt(<!ARGUMENT_TYPE_MISMATCH!>foo<!>)
        }
        if (this.foo != null) {
            useInt(foo)
        }
        if (foo != null) {
            useInt(this.foo)
        }
    }

    fun useInt(i: Int) = i
}