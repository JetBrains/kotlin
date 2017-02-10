package foo

class A {
    private annotation class B

    class C {
        @B
        fun foo() {}
    }
}
// ANNOTATION: foo.A.B
// SEARCH: method:foo
