fun foo() {
    open class A {
        open fun foo() {}
    }

    object : A() {
        override fun fo<caret>o() {}
    }
}