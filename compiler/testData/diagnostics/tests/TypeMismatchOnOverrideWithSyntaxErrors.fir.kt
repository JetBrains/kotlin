abstract class A {
    abstract var x: Int;
    abstract fun foo() : Int;
}

abstract class C : A() {
    override abstract var x: String =<!SYNTAX!><!> <!SYNTAX!>?<!>
    override abstract fun foo(): String =<!SYNTAX!><!> <!SYNTAX!>?<!>
}
