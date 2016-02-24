// FILE: A.kt

class A {
    companion object {
        fun foo() = 42
        val bar = "OK"
    }
}

// FILE: B.kt

fun main(args: Array<String>) {
    if (A.foo() != 42) throw Exception()
    if (A.bar != "OK") throw Exception()
}
