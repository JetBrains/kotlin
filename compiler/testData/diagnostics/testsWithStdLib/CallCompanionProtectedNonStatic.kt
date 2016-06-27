open class VeryBase {
    protected fun baz() {}
}

open class Base {
    protected fun foo() { 
        bar() // Ok
        baz() // Ok
    }

    inner class Inner {
        fun fromInner() {
            foo() // Ok
            bar() // Ok
            gav() // Ok
            baz() // Ok
        }
    }

    class NestedDerived : Base() {
        fun fromNestedDerived() {
            foo() // Ok
            bar() // Ok
            gav() // Ok
            baz() // Ok
        }
    }
    
    companion object : VeryBase() {
        var prop = 42
            protected set

        protected fun bar() {}

        @JvmStatic protected fun gav() {}       

        class Nested {
            fun fromNested() {
                bar() // Ok
                gav() // Ok
            }
        }
    }
}

class Derived : Base() {
    fun test() {
        foo() // Ok
        gav() // Ok
        <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>bar<!>()
        <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>baz<!>()
        <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>prop<!> = 0
    }

    inner class DerivedInner {
        fun fromDerivedInner() {
            foo() // Ok
            gav() // Ok
            <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>bar<!>()
            <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>baz<!>()
            <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>prop<!> = 0
        }
    }

    companion object {
        fun test2() {
            gav() // Ok
            <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>bar<!>()
            <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>baz<!>()
            <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>prop<!> = 0
        }
    }
}

class Other {
    fun test(base: Base, derived: Derived) {
        base.<!INVISIBLE_MEMBER!>foo<!>()
        base.<!UNRESOLVED_REFERENCE!>gav<!>()
        base.<!UNRESOLVED_REFERENCE!>bar<!>()
        derived.<!INVISIBLE_MEMBER!>foo<!>()
        derived.<!UNRESOLVED_REFERENCE!>gav<!>()
        derived.<!UNRESOLVED_REFERENCE!>bar<!>()
    }
}

fun top(base: Base, derived: Derived) {
    base.<!INVISIBLE_MEMBER!>foo<!>()
    base.<!UNRESOLVED_REFERENCE!>bar<!>()
    base.<!UNRESOLVED_REFERENCE!>gav<!>()
    derived.<!INVISIBLE_MEMBER!>foo<!>()
    derived.<!UNRESOLVED_REFERENCE!>bar<!>()
    derived.<!UNRESOLVED_REFERENCE!>gav<!>()
}
