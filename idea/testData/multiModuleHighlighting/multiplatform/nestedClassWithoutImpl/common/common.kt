package a

header class A {
    class Nested
}

header class B {
    class Nested {
        fun foo(s: String)
    }
}

header class C {
    <error>header</error> inner class Inner
}
