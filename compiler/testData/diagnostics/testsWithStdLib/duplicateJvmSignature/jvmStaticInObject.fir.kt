// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Base {
    fun `foo$default`(i: Int, mask: Int, mh: Any) {}
}

object Derived : Base() {
    @JvmStatic fun foo(i: Int = 0) {}
}
