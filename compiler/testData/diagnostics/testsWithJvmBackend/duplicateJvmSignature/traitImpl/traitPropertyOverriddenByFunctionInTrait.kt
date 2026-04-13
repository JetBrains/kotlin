interface T {
    val x: Int
        get() = 1
}

<!CONFLICTING_JVM_DECLARATIONS!><!>interface C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX() = 1<!>
}
