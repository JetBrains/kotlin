trait T {
    fun getX() = 1
}

class C : T {
    val x: Int
        <!CONFLICTING_JVM_DECLARATIONS!>get() = 1<!>
}