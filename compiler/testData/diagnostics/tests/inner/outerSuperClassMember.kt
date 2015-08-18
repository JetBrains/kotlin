open class Base {
    fun foo() {}
}

class Derived : Base() {
    class Nested {
        fun bar() = <!UNRESOLVED_REFERENCE!>foo<!>()
    }
}