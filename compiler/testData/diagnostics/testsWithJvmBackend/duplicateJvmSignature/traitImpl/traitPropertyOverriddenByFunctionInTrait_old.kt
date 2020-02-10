// TARGET_BACKEND: JVM_OLD

interface T {
    val x: Int
        get() = 1
}

interface <!CONFLICTING_JVM_DECLARATIONS!>C<!> : T {
    <!ACCIDENTAL_OVERRIDE, ACCIDENTAL_OVERRIDE, CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
}
