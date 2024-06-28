// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER

open class Base {
    fun `foo$default`(i: Int, mask: Int, mh: Any) {}
}

object Derived : Base() {
    @JvmStatic <!ACCIDENTAL_OVERRIDE!>fun foo(i: Int = 0) {}<!>
}
