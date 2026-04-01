// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: KotlinClass.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicInt

open class KotlinClass {
    open fun foo(a: AtomicInt): String {
        return "not OK"
    }
    open val a: AtomicInt
        get() = AtomicInt(0)
}

// FILE: JavaClassWithExplicitOverride.java
import java.util.concurrent.atomic.*;

public class JavaClassWithExplicitOverride extends KotlinClass {
    @Override
    public String foo(AtomicInteger a) {
        return "OK";
    }

    @Override
    public AtomicInteger getA() {
        return new AtomicInteger(1);
    }
}

// FILE: test.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import JavaClassWithExplicitOverride
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicInt

fun box(): String {
    val x = JavaClassWithExplicitOverride()
    val y: AtomicInt = x.a
    return if (
        (y.load() == 1) &&
        (x.foo(AtomicInteger(0)) == "OK") &&
        (x.foo(AtomicInt(0)) == "OK")
        ) "OK"
    else "not OK"
}