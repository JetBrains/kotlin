// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base {
    open fun `foo$default`(d: Derived, i: Int, mask: Int) {}
}

class Derived : Base() {
    <!ACCIDENTAL_OVERRIDE!>fun foo(i: Int = 0)<!> {}
}
