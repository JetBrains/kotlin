// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JavaOverloadsChild.java
public class JavaOverloadsChild extends OverloadsWithAtomicIntAndInteger { }

// FILE: test.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import JavaOverloadsChild
import kotlin.concurrent.atomics.AtomicInt
import java.util.concurrent.atomic.AtomicInteger

open class OverloadsWithAtomicIntAndInteger {
    open fun f(x: AtomicInt): Int = 1
    open fun f(y: AtomicInteger): String = "2"
}

class KotlinOverloadsChild: OverloadsWithAtomicIntAndInteger()

fun box(): String {
    val x = OverloadsWithAtomicIntAndInteger()
    val y = KotlinOverloadsChild()
    val z = JavaOverloadsChild()
    return if (
        (x.f(AtomicInteger(1)) == "2") &&
        (x.f(AtomicInt(1)) == 1) &&
        (y.f(AtomicInteger(1)) == "2") &&
        (y.f(AtomicInt(1)) == 1) &&
        (z.f(AtomicInteger(1)) == "2") &&
        (y.f(AtomicInt(1)) == 1)
    ) "OK"
    else "not OK"
}