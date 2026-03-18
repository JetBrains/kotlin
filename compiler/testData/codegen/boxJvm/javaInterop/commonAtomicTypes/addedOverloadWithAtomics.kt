// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JavaBase.java
import java.util.concurrent.atomic.AtomicInteger;

public class JavaBase {
    public String foo(AtomicInteger a) {
        return "1";
    }
}

// FILE: test.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicInt
import java.util.concurrent.atomic.AtomicInteger

class KotlinChildFromBase : JavaBase() {
    fun foo(a: AtomicInt): Int = 2
}

fun box(): String {
    return if (
        (KotlinChildFromBase().foo(AtomicInteger(1)) == "1") &&
        (KotlinChildFromBase().foo(AtomicInt(1)) == 2)
        ) "OK"
    else "not OK"
}