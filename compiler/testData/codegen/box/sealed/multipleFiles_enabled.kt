// ISSUE: KT-13495
// IGNORE_BACKEND_FIR: JVM_IR
// !LANGUAGE: +FreedomForSealedClasses

// FILE: a.kt

sealed class Base {
    class A : Base()
}

// FILE: b.kt

class B : Base()

// FILE: c.kt

fun getLetter(base: Base): String = when (base) {
    is Base.A -> "O"
    is B -> "K"
}

fun box(): String {
    return getLetter(Base.A()) + getLetter(B())
}
