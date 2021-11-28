// MODULE: lib
// FILE: A.kt

class A {
    companion object {
        fun foo() = 42
        val bar = "OK"
    }
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    if (A.foo() != 42) return "Fail foo"
    return A.bar
}
