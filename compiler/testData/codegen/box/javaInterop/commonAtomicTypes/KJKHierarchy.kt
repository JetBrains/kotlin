// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: KotlinClass.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicInt

open class KotlinClass {
    open fun foo(a: AtomicInt): String {
        return "KotlinClass"
    }
    open val a: AtomicInt = AtomicInt(0)
}

// FILE: JavaClassWithFakeOverride.java

public class JavaClassWithFakeOverride extends KotlinClass {}

// FILE: test.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import JavaClassWithFakeOverride
import kotlin.concurrent.atomics.*

class KotlinChildWithFakeOverride: JavaClassWithFakeOverride()

class KotlinChildWithExplicitOverride: JavaClassWithFakeOverride() {
    override fun foo(i: AtomicInt): String {
        return "KotlinChildWithExplicitOverride"
    }
    override val a: AtomicInt
        get() = AtomicInt(1)
}

fun box(): String {
    val child1 = KotlinChildWithFakeOverride()
    val child2 = KotlinChildWithExplicitOverride()
    return if (
        (child1.foo(AtomicInt(0)) == "KotlinClass") &&
        (child2.foo(AtomicInt(0)) == "KotlinChildWithExplicitOverride") &&
        (child1.a.load() == 0) &&
        (child2.a.load() == 1)
    ) "OK"
    else "not OK"
}