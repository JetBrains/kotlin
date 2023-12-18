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
        x.<!INVISIBLE_MEMBER!>foo<!>()
        x.<!INVISIBLE_MEMBER!>bar<!>()

        x.<!INVISIBLE_MEMBER!>x<!> = x.<!INVISIBLE_MEMBER!>x<!> + 1
        <!INVISIBLE_SETTER!>x.y<!> = x.y + 1

        if (x is Derived) {
            <!DEBUG_INFO_SMARTCAST!>x<!>.foo()
            <!DEBUG_INFO_SMARTCAST!>x<!>.bar()
            <!DEBUG_INFO_SMARTCAST!>x<!>.baz(x)

            <!DEBUG_INFO_SMARTCAST!>x<!>.x = <!DEBUG_INFO_SMARTCAST!>x<!>.x + 1
            // TODO: Should be smart cast
            <!INVISIBLE_SETTER!>x.y<!> = x.y + 1
        }
    }

    protected fun baz2(x: Base?) {
        x<!UNSAFE_CALL!>.<!><!INVISIBLE_MEMBER!>foo<!>()
        x<!UNSAFE_CALL!>.<!><!INVISIBLE_MEMBER!>bar<!>()

        x<!UNSAFE_CALL!>.<!><!INVISIBLE_MEMBER!>x<!> = x<!UNSAFE_CALL!>.<!><!INVISIBLE_MEMBER!>x<!> + 1
        <!INVISIBLE_SETTER!>x<!UNSAFE_CALL!>.<!>y<!> = x<!UNSAFE_CALL!>.<!>y + 1

        if (x is Derived) {
            <!DEBUG_INFO_SMARTCAST!>x<!>.foo()
            <!DEBUG_INFO_SMARTCAST!>x<!>.bar()
            <!DEBUG_INFO_SMARTCAST!>x<!>.baz(<!DEBUG_INFO_SMARTCAST!>x<!>)

            <!DEBUG_INFO_SMARTCAST!>x<!>.x = <!DEBUG_INFO_SMARTCAST!>x<!>.x + 1
            <!DEBUG_INFO_SMARTCAST!>x<!>.y = <!DEBUG_INFO_SMARTCAST!>x<!>.y + 1
        }
    }
}
