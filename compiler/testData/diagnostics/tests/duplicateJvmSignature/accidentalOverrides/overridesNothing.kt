interface B {
    fun getX() = 1
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : B {
    <!NOTHING_TO_OVERRIDE!>override<!> val x = 1
}