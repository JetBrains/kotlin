interface T {
    val x: Int
        <!CONFLICTING_JVM_DECLARATIONS!>get() = 1<!>
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX() = 1<!>
}
