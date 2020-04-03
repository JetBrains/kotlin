// TARGET_BACKEND: JVM_OLD
interface T {
    val x: Int
        get() = 1
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : T {
    <!ACCIDENTAL_OVERRIDE, CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
}