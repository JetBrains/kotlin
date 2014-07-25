interface T {
    val x: Int
        get() = 1
}

interface <!CONFLICTING_JVM_DECLARATIONS!>C<!> : T {
    fun getX() = 1
}
