// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// JSPECIFY_STATE: strict
// JSR305_GLOBAL_REPORT: strict
// WITH_JSR305_TEST_ANNOTATIONS
// ISSUE: KT-68358

// MODULE: lib
// FILE: test/package-info.java
@NonNullApi
package test;

// FILE: test/Base.java
package test;

public interface Base<T> {
    public T foo(T x);
}

// FILE: test/Impl.kt
package test

interface Derived : Base<Long>

object SomeImpl : Derived {
    override fun foo(x: Long): Long {
        return x + x
    }
}

class Impl(val delegate: Derived) : Derived by delegate

// MODULE: main(lib)
// FILE: main.kt
import test.*

fun test(s: Impl): Long {
    val x = 10L
    return s.foo(x)
}

fun box(): String {
    val x = test(Impl(SomeImpl))
    return if (x == 20L) {
        "OK"
    } else {
        "Fail: $x"
    }
}
