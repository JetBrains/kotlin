// !JVM_TARGET: 1.8
// FILE: 1.kt
interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    fun test() {
    }
}

// FILE: 2.kt
interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>B<!> : A {

}

interface C : B {
    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    override fun test() {
        super.test()
    }
}

open class Foo : B {
    override fun test() {
        super.test()
    }
}
open class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Foo2<!> : B

open class Bar : Foo2() {
    override fun test() {
        super.test()
    }
}

open class Bar2 : Bar() {
    override fun test() {
        super.test()
    }
}

class <!JVM_DEFAULT_THROUGH_INHERITANCE!>ManySupers<!>: Foo2(), B {
    fun foo() {
        super<Foo2>.test()
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>B<!>>.test()
        <!AMBIGUOUS_SUPER!>super<!>.test()
    }
}

class <!JVM_DEFAULT_THROUGH_INHERITANCE!>ManySupers2<!>: Foo2(), C {
    fun foo() {
        super<Foo2>.test()
        super<C>.test()
        <!AMBIGUOUS_SUPER!>super<!>.test()
    }
}

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class <!JVM_DEFAULT_THROUGH_INHERITANCE!>ManySupers3<!><!>: Bar2(), C {
    fun foo() {
        super<Bar2>.test()
        super<C>.test()
        <!AMBIGUOUS_SUPER!>super<!>.test()
    }
}
