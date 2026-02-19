interface T {
    fun getX() = 1
}

<!CONFLICTING_JVM_DECLARATIONS!><!>interface C : T {
    val x: Int
        <!ACCIDENTAL_OVERRIDE!>get() = 1<!>
}
