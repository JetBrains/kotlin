trait B {
    fun getX() = 1
}

trait D {
    val x: Int
}

class <!CONFLICTING_JVM_DECLARATIONS!>C(d: D)<!> : D by d, B {
}