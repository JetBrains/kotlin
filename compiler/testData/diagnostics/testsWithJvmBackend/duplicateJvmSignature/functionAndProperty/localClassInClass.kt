// FIR_IDENTICAL

class Outer {
    fun foo() {
        class C {
            <!CONFLICTING_JVM_DECLARATIONS!>val x = 1<!>
            <!CONFLICTING_JVM_DECLARATIONS!>fun getX() = 1<!>
        }
    }
}
