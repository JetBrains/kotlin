interface T {
    fun getX() = 1
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : T {
    <!CONFLICTING_JVM_DECLARATIONS!>val x<!> = 1
}