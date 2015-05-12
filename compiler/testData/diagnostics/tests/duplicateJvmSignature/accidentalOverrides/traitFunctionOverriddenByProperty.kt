interface T {
    fun getX() = 1
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : T {
    val x: Int
        <!CONFLICTING_JVM_DECLARATIONS!>get()<!> = 1
}