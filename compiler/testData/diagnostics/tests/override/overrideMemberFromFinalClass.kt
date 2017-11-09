class Foo {
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> fun openFoo() {}
    fun finalFoo() {}
}

class Bar : <!FINAL_SUPERTYPE!>Foo<!>() {
    override fun openFoo() {}
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun finalFoo() {}
}


open class A1 {
    open fun foo() {}
}

class B1 : A1()
class C1 : <!FINAL_SUPERTYPE!>B1<!>() {
    override fun foo() {}
}

abstract class A2 {
    abstract fun foo()
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class B2<!> : A2()
class C2 : <!FINAL_SUPERTYPE!>B2<!>() {
    override fun foo() {}
}