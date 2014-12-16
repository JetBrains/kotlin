class A {
    fun bar() {

    }

    fun baz() {

    }

    fun foo() {
        <selection>::bar</selection>
        ::baz
        A::bar
        A::baz
    }
}