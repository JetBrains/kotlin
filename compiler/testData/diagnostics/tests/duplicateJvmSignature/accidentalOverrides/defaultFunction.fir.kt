// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base {
    open fun `foo$default`(d: Derived, i: Int, mask: Int, mh: Any) {}
}

class Derived : Base() {
    fun foo(i: Int = 0) {}
}
