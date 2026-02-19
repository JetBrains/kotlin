// SKIP_KT_DUMP
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: JVM_IR
// LANGUAGE: +ExplicitBackingFields

interface I

value class V(val x: Int) : I  {
    constructor() : this(42) {}
}

open class A {
    val p0: Any
        field = 42

    val p1: Any
        field = V(42)

    val p2: I
        field = V(42)

    val p3: I
        field = V()

    val p4: I field: V

    init {
        p4 = V(42)
    }
}

class B : A() {
    val p = p4
}