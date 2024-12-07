// TARGET_BACKEND: JVM
// MODULE: lib

// FILE: A.kt
abstract class A {
    private val x = object {
        fun foo() = "OK"
    }

    protected val y = x.foo()
}

// MODULE: main(lib)
// FILE: B.kt

class B : A() {
    val z = y
}

fun box() = B().z