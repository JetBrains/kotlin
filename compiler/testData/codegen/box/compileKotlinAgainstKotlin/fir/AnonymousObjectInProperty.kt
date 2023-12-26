// TARGET_BACKEND: JVM
// MODULE: lib
// JVM_ABI_K1_K2_DIFF: KT-63868

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