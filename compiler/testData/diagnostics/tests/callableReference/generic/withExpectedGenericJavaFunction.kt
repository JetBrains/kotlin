// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-71228
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

// FILE: Base.java

import java.util.function.Function;

public class Base {
    protected abstract <T> T foo(Function<T, Object> f);
}

// FILE: Derived.kt

abstract class Derived : Base() {
    fun bar() {
        val res: Int? = foo(::baz)
    }

    abstract fun baz(x: Int?)
    abstract fun baz(y: String?)
}
