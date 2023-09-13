// MODULE: lib
// FILE: lib.kt

// KT-41765

interface X {
    override fun toString() :String
}

interface Y

abstract class A: Y, X

// MODULE: main(lib)
// FILE: main.kt

class B: A() {
    override fun toString() = "BBB"
}

fun box(): String {
    if (B().toString() == "BBB") return "OK"
    return "FAIL"
}

