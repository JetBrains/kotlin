// TARGET_BACKEND: JVM
// MODULE: lib
// JVM_DEFAULT_MODE: disable
// FILE: lib.kt

interface A {
    fun f(x: Int): Int
}

interface B : A {
    override fun f(x: Int): Int = x
}

// MODULE: main(lib)
// JVM_DEFAULT_MODE: no-compatibility
// FILE: main.kt

fun interface C : B {
    fun g(): String
}

fun box(): String {
    val e1 = object : C {
        override fun g(): String = "O"
    }
    if (e1.f(1) != 1) return "Fail 1"

    val e2 = C { "K" }
    if (e2.f(2) != 2) return "Fail 2"

    return e1.g() + e2.g()
}
