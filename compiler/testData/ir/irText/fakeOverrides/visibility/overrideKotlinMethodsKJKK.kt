// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// ^ ISSUE: KT-65679

// FILE: J.java
public class J extends B {}


// FILE: test.kt
abstract class A {
    open public fun funA1() {}
    open protected fun funA2() {}
    open internal fun funA3() {}
}

abstract class B: A() {
    override fun funA1() {}
    override fun funA2() {}
    override fun funA3() {}
}

class C: J() {}

fun test(c: C) {
    c.funA1()
    c.funA3()
}
