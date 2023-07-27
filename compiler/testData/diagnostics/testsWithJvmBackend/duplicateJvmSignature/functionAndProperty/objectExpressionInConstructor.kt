// FIR_IDENTICAL

class Outer {
    val x = object {
        <!CONFLICTING_JVM_DECLARATIONS!>val x = 1<!>
        <!CONFLICTING_JVM_DECLARATIONS!>fun getX() = 1<!>
    }
}
