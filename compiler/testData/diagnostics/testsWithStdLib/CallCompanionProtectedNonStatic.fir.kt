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
        bar()
        baz()
        prop = 0
    }

    inner class DerivedInner {
        fun fromDerivedInner() {
            foo() // Ok
            gav() // Ok
            bar()
            baz()
            prop = 0
        }
    }

    companion object {
        fun test2() {
            gav() // Ok
            bar()
            baz()
            prop = 0
        }
    }
}

class Other {
    fun test(base: Base, derived: Derived) {
        base.<!INVISIBLE_REFERENCE!>foo<!>()
        base.<!UNRESOLVED_REFERENCE!>gav<!>()
        base.<!UNRESOLVED_REFERENCE!>bar<!>()
        derived.<!INVISIBLE_REFERENCE!>foo<!>()
        derived.<!UNRESOLVED_REFERENCE!>gav<!>()
        derived.<!UNRESOLVED_REFERENCE!>bar<!>()
    }
}

fun top(base: Base, derived: Derived) {
    base.<!INVISIBLE_REFERENCE!>foo<!>()
    base.<!UNRESOLVED_REFERENCE!>bar<!>()
    base.<!UNRESOLVED_REFERENCE!>gav<!>()
    derived.<!INVISIBLE_REFERENCE!>foo<!>()
    derived.<!UNRESOLVED_REFERENCE!>bar<!>()
    derived.<!UNRESOLVED_REFERENCE!>gav<!>()
}
