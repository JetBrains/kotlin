class Outer1 {
    class Nested

    class C1 { val b = Nested() }
    class C2(val b: Any = Nested())
    inner class C3 { val b = Nested() }
    inner class C4(val b: Any = Nested())

    inner class Inner

    class C5 { val b = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>() }
    class C6(val b: Any = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>())
    inner class C7 { val b = Inner() }
    inner class C8(val b: Any = Inner())
}


class Outer2 {
    class Nested {
        fun foo() = Outer2()
        fun bar() = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
    }
    inner class Inner {
        fun foo() = Outer2()
        fun bar() = Nested()
    }

    fun foo() {
        Nested()
        Inner()
    }
}
