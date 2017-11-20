package a

expect class A {
    class Nested
}

expect class B {
    class Nested {
        fun foo(s: String)
    }
}

expect class C {
    <error>header</error> inner class Inner
}
