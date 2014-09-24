// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.platform.platformStatic

open class Base {
    fun `foo$default`(i: Int, mask: Int) {}
}

object Derived : Base() {
    <!ACCIDENTAL_OVERRIDE!>platformStatic fun foo(i: Int = 0)<!> {}
}
