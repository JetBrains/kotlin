// TARGET_BACKEND: JVM_OLD
interface T {
    fun getX() = 1
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : T {
    val x: Int
        <!ACCIDENTAL_OVERRIDE, CONFLICTING_JVM_DECLARATIONS!>get()<!> = 1
}