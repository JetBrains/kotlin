// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB

// FILE: KotlinClass.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

open class KotlinClass {
    open fun foo(a: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Int><!>) { }
}

// FILE: JavaClass.java
import java.util.concurrent.atomic.AtomicReference;

public class JavaClass extends KotlinClass {
    @Override
    public void foo(AtomicReference a) { }
}

// FILE: test.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

fun usage(a: JavaClass) {
    a.foo(java.util.concurrent.atomic.AtomicReference(""))
    a.foo(<!ARGUMENT_TYPE_MISMATCH, ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference(1)<!>)
}