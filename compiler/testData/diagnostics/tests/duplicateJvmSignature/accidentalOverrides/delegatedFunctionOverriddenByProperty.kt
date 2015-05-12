interface B {
    fun getX() = 1
}

interface D {
    val x: Int
}

class <!CONFLICTING_JVM_DECLARATIONS!>C(d: D)<!> : D by d, B {
}