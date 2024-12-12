// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB

// FILE: KotlinClass.kt
import kotlin.concurrent.atomics.AtomicInt

open class KotlinClass {
    open fun foo(a: AtomicInt) { }
    open val a: AtomicInt = AtomicInt(0)
}

// FILE: JavaClassWithFakeOverride.java

public class JavaClassWithFakeOverride extends KotlinClass { }

// FILE: test.kt
import JavaClassWithFakeOverride
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicInt

fun usage(x: JavaClassWithFakeOverride) {
    x.foo(AtomicInt(0))
    x.foo(<!ARGUMENT_TYPE_MISMATCH!>AtomicInteger(0)<!>)
    val t1: AtomicInt = x.a
    val t2: AtomicInteger = <!INITIALIZER_TYPE_MISMATCH!>x.a<!>
}
