// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.jvm.jvmStatic

open class Base {
    fun `foo$default`(i: Int, mask: Int) {}
}

object Derived : Base() {
    <!ACCIDENTAL_OVERRIDE!>jvmStatic fun foo(i: Int = 0)<!> {}
}
