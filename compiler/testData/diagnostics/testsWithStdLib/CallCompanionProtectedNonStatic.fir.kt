open class VeryBase {
    protected fun baz() {}
}

open class Base {
    protected fun foo() { 
        bar() // Ok
        <!INAPPLICABLE_CANDIDATE!>baz<!>() // Ok
    }

    inner class Inner {
        fun fromInner() {
            foo() // Ok
            bar() // Ok
            gav() // Ok
            <!INAPPLICABLE_CANDIDATE!>baz<!>() // Ok
        }
    }

    class NestedDerived : Base() {
        fun fromNestedDerived() {
            foo() // Ok
            bar() // Ok
            gav() // Ok
            <!INAPPLICABLE_CANDIDATE!>baz<!>() // Ok
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
        <!INAPPLICABLE_CANDIDATE!>gav<!>() // Ok
        <!INAPPLICABLE_CANDIDATE!>bar<!>()
        <!INAPPLICABLE_CANDIDATE!>baz<!>()
        prop = 0
    }

    inner class DerivedInner {
        fun fromDerivedInner() {
            foo() // Ok
            <!INAPPLICABLE_CANDIDATE!>gav<!>() // Ok
            <!INAPPLICABLE_CANDIDATE!>bar<!>()
            <!INAPPLICABLE_CANDIDATE!>baz<!>()
            prop = 0
        }
    }

    companion object {
        fun test2() {
            <!INAPPLICABLE_CANDIDATE!>gav<!>() // Ok
            <!INAPPLICABLE_CANDIDATE!>bar<!>()
            <!INAPPLICABLE_CANDIDATE!>baz<!>()
            prop = 0
        }
    }
}

class Other {
    fun test(base: Base, derived: Derived) {
        base.<!INAPPLICABLE_CANDIDATE!>foo<!>()
        base.<!UNRESOLVED_REFERENCE!>gav<!>()
        base.<!UNRESOLVED_REFERENCE!>bar<!>()
        derived.<!INAPPLICABLE_CANDIDATE!>foo<!>()
        derived.<!UNRESOLVED_REFERENCE!>gav<!>()
        derived.<!UNRESOLVED_REFERENCE!>bar<!>()
    }
}

fun top(base: Base, derived: Derived) {
    base.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    base.<!UNRESOLVED_REFERENCE!>bar<!>()
    base.<!UNRESOLVED_REFERENCE!>gav<!>()
    derived.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    derived.<!UNRESOLVED_REFERENCE!>bar<!>()
    derived.<!UNRESOLVED_REFERENCE!>gav<!>()
}
