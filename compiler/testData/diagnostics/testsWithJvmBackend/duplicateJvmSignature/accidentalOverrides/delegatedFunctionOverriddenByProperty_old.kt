// TARGET_BACKEND: JVM_OLD
interface B {
    fun getX() = 1
}

interface D {
    val x: Int
}

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>C(d: D)<!> : D by d, B