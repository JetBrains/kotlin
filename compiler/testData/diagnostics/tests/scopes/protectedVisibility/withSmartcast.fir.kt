// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT

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
        x.<!INVISIBLE_REFERENCE!>x<!>++
        x.<!INVISIBLE_REFERENCE!>x<!> += 1
        x.<!INVISIBLE_SETTER!>y<!> = x.y + 1
        x.<!INVISIBLE_SETTER!>y<!>++
        x.<!INVISIBLE_SETTER!>y<!> += 1

        if (x is Derived) {
            x.foo()
            x.bar()
            x.baz(x)

            x.x = x.x + 1
            x.x++
            x.x += 1
            // TODO: Should be smart cast
            x.<!INVISIBLE_SETTER!>y<!> = x.y + 1
            x.<!INVISIBLE_SETTER!>y<!>++
            x.<!INVISIBLE_SETTER!>y<!> += 1
        }
    }

    protected fun baz2(x: Base?) {
        x.<!INVISIBLE_REFERENCE!>foo<!>()
        x.<!INVISIBLE_REFERENCE!>bar<!>()

        x.<!INVISIBLE_REFERENCE!>x<!> = x.<!INVISIBLE_REFERENCE!>x<!> + 1
        x.<!INVISIBLE_REFERENCE!>x<!>++
        x.<!INVISIBLE_REFERENCE!>x<!> += 1
        x<!UNSAFE_CALL!>.<!><!INVISIBLE_SETTER!>y<!> = x<!UNSAFE_CALL!>.<!>y + 1
        x<!UNSAFE_CALL!>.<!><!INVISIBLE_SETTER!>y<!>++
        x<!UNSAFE_CALL!>.<!><!INVISIBLE_SETTER!>y<!> += 1

        if (x is Derived) {
            x.foo()
            x.bar()
            x.baz(x)

            x.x = x.x + 1
            x.x++
            x.x += 1
            x.y = x.y + 1
            x.y++
            x.y += 1
        }
    }
}
