// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB

// FILE: JavaClass.java
import java.util.concurrent.atomic.*;

public class JavaClass {
    public void foo(AtomicInteger i) {}
    public AtomicInteger a = new AtomicInteger(0);
}

// FILE: test.kt
import JavaClass
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicInt

class KotlinClassWithFakeOverride : JavaClass()

class KotlinClassWithExplicitOverride : JavaClass() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(i: AtomicInt) { }

    override fun foo(i: AtomicInteger) { }

    <!NOTHING_TO_OVERRIDE!>override<!> val <!PROPERTY_HIDES_JAVA_FIELD!>a<!>: AtomicInt = AtomicInt(0)
}

fun usage(a: KotlinClassWithFakeOverride) {
    a.foo(AtomicInteger(0))
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>AtomicInt(0)<!>)
    val t1: AtomicInteger = a.a
    val t2: AtomicInt = <!INITIALIZER_TYPE_MISMATCH!>a.a<!>
}