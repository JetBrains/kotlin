// WITH_STDLIB

open class Base {
    fun foo() {}
}

class Derived : Base() {
    companion object {
        @JvmStatic <!ACCIDENTAL_OVERRIDE!>fun foo() {}<!>
    }
}
