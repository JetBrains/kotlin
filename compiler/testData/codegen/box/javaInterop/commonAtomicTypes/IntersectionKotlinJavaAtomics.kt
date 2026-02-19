// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: KotlinInterface.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicInt

interface KotlinInterface {
    fun foo(a: AtomicInt): String {
        return "KotlinInterface"
    }
    val a: AtomicInt
        get() = AtomicInt(0)
}

// FILE: JavaClass.java
import java.util.concurrent.atomic.*;

public class JavaClass {
    public String foo(AtomicInteger i) {
        return "JavaClass";
    }
    public AtomicInteger a = new AtomicInteger(1);
}

// FILE: JavaIntersection.java
import java.util.concurrent.atomic.*;

public class JavaIntersection extends JavaClass implements KotlinInterface {
    @Override
    public AtomicInteger getA() {
        return new AtomicInteger(2);
    }
}

// FILE: JavaIntersectionWithExplicitOverride.java
import java.util.concurrent.atomic.*;

public class JavaIntersectionWithExplicitOverride extends JavaClass implements KotlinInterface {
    @Override
    public String foo(AtomicInteger a) {
        return "JavaIntersectionWithExplicitOverride";
    }
    @Override
    public AtomicInteger getA() {
        return new AtomicInteger(3);
    }
}

// FILE: test.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import JavaClass
import kotlin.concurrent.atomics.AtomicInt
import java.util.concurrent.atomic.AtomicInteger

class KotlinIntersection: KotlinInterface, JavaClass()

fun box(): String {
    val intersection1 = KotlinIntersection()
    val intersection2 = JavaIntersection()
    val intersection3 = JavaIntersectionWithExplicitOverride()
    return if (
        (intersection1.foo(AtomicInt(0)) == "KotlinInterface") &&
        (intersection1.foo(AtomicInteger(0)) == "KotlinInterface") &&
        (intersection2.foo(AtomicInt(0)) == "JavaClass") &&
        (intersection2.foo(AtomicInteger(0)) == "JavaClass") &&
        (intersection3.foo(AtomicInt(0)) == "JavaIntersectionWithExplicitOverride") &&
        (intersection3.foo(AtomicInteger(0)) == "JavaIntersectionWithExplicitOverride") &&
        (intersection1.a.get() == 1) &&
        (intersection2.a.get() == 1) &&
        (intersection3.a.get() == 1)
    ) "OK"
    else "not OK"
}