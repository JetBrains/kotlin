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

public class JavaClassWithFakeOverride extends KotlinClass {}

// FILE: test.kt
import JavaClassWithFakeOverride
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicInt

class KotlinChildWithFakeOverride: JavaClassWithFakeOverride()

class KotlinChildWithExplicitOverride: JavaClassWithFakeOverride() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(i: AtomicInteger) { }
    override val a: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>AtomicInteger<!>
        get() = AtomicInteger(1)
}

fun usage(a:KotlinChildWithFakeOverride) {
    a.foo(AtomicInt(0))
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>AtomicInteger(0)<!>)
    val t1: AtomicInt = a.a
    val t2: AtomicInteger = <!INITIALIZER_TYPE_MISMATCH!>a.a<!>
}
