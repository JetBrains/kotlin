open class Base {
    open protected fun foo() {}
    open protected fun bar() {}

    open protected var x: Int = 1
    open var y: Int = 1
        protected set
}

class Derived : Base() {
    override fun bar() { }

    protected fun baz(x: Base) {
        x.<!INVISIBLE_REFERENCE!>foo<!>()
        x.<!INVISIBLE_REFERENCE!>bar<!>()

        x.<!INVISIBLE_REFERENCE!>x<!> = x.<!INVISIBLE_REFERENCE!>x<!> + 1
        x.y = x.y + 1

        if (x is Derived) {
            x.foo()
            x.bar()
            x.baz(x)

            x.x = x.x + 1
            // TODO: Should be smart cast
            x.y = x.y + 1
        }
    }
}
