interface T {
    val x: Int
        get() = 1
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : T {
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
}