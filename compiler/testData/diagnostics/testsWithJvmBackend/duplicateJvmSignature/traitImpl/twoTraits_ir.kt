// TARGET_BACKEND: JVM_IR

interface T1 {
    fun getX() = 1
}

interface T2 {
    val x: Int
        get() = 1
}

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>C<!> : T1, T2 {
}