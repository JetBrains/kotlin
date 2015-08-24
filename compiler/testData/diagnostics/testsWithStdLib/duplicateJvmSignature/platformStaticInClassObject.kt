import kotlin.jvm.jvmStatic

open class Base {
    fun foo() {}
}

class Derived : Base() {
    companion object {
        <!ACCIDENTAL_OVERRIDE!>jvmStatic fun foo()<!> {}
    }
}
