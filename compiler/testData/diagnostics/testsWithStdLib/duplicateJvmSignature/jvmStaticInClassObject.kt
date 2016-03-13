open class Base {
    fun foo() {}
}

class Derived : Base() {
    companion object {
        <!ACCIDENTAL_OVERRIDE!>@JvmStatic fun foo()<!> {}
    }
}
