open class Base {
    fun foo() {}
}

class Derived : Base() {
    class Nested {
        fun bar() = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>foo()<!>
    }
}
