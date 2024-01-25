// ISSUE: KT-65058

open class A {
    protected fun foo() {
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun bar() = object : A() {
    fun baz() {
        foo()
    }
}
