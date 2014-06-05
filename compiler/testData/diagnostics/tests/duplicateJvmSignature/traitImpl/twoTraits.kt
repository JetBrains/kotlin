trait T1 {
    fun getX() = 1
}

trait T2 {
    val x: Int
        get() = 1
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : T1, T2 {
}