// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JavaClass.java
import java.util.concurrent.atomic.*;

public class JavaClass {
    public AtomicInteger a = new AtomicInteger(0);
    public AtomicIntegerArray b = new AtomicIntegerArray(new int[]{1, 1, 1});
}

// FILE: test.kt

@file:OptIn(ExperimentalAtomicApi::class)

import JavaClass
import kotlin.concurrent.atomics.asKotlinAtomic
import kotlin.concurrent.atomics.asKotlinAtomicArray
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicIntArray
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class KotlinClass {
    fun foo(a: AtomicInt): String {
        return "O"
    }
    fun bar(a: AtomicIntArray): String {
        return "K"
    }
}

fun usage(a: KotlinClass): String {
    return a.foo(JavaClass().a.asKotlinAtomic())+ a.bar(JavaClass().b.asKotlinAtomicArray())
}

fun box(): String {
    return usage(KotlinClass())
}