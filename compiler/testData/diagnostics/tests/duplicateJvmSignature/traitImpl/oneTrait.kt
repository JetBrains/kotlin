trait T {
    fun getX() = 1
}

class C : T {
    <!CONFLICTING_JVM_DECLARATIONS!>val x<!> = 1
}