// WITH_STDLIB

// FILE: KotlinInterface.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicInt

interface KotlinInterface {
    fun foo(a: AtomicInt) { }
    val a: AtomicInt
        get() = AtomicInt(0)
}

// FILE: JavaClass.java
import java.util.concurrent.atomic.*;

public class JavaClass {
    public void foo(AtomicInteger a) { }
    public AtomicInteger a = new AtomicInteger(1);
}

// FILE: test.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicInt

class IntersectionWithExplicitOverride: KotlinInterface, JavaClass() {
    override <!ACCIDENTAL_OVERRIDE!>fun foo(a: AtomicInt) {}<!>
}

class IntersectionWithExplicitOverride2: KotlinInterface, JavaClass() {
    override <!ACCIDENTAL_OVERRIDE!>fun foo(a: AtomicInteger) {}<!>
}