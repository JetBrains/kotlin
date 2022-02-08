open class BaseClass() {
    protected class Nested(val x: Int, protected val y: Int)

    protected fun foo() = Nested(1, 2)
}

class Foo : BaseClass() {
    fun bar() {
        val f = foo()
        f.x
        f.<!INVISIBLE_REFERENCE!>y<!>
    }
}
