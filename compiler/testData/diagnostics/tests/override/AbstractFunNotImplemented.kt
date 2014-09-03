abstract class A {
    abstract fun foo(): Int
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class B<!>() : A() {
}
