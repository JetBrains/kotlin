import kotlin.platform.platformStatic

open class Base {
    fun foo() {}
}

class Derived : Base() {
    companion object {
        <!ACCIDENTAL_OVERRIDE!>platformStatic fun foo()<!> {}
    }
}
