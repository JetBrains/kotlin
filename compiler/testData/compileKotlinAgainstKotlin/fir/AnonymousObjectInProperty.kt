// TARGET_BACKEND: JVM
// FILE: A.kt

abstract class A {
    private val x = object {
        fun foo() = "OK"
    }

    protected val y = x.foo()
}

// FILE: B.kt

class B : A() {
    val z = y
}

fun box() = B().z