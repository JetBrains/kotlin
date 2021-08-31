fun foo() {
    open class A {
        open fun foo() {}
    }

    class B : A() {
        override fun fo<caret>o() {}
    }
}