// ISSUE: KT-66909
// TARGET_BACKEND: JVM
// WITH_STDLIB
// DUMP_IR
// FIR_IDENTICAL

// IGNORE_BACKEND_K2: ANY
// ^ This test results in compilation error on K2 (KT-66954)

// FILE: A.java
import org.jetbrains.annotations.NotNull;
import kotlin.jvm.functions.Function0;
import kotlin.Unit;

public class A {
    public static @NotNull Function0<Unit> foo;
}

// FILE: main.kt
import kotlin.test.*

fun nullableUnit(): Unit? = null

fun box(): String {
    A.foo = l@ { return@l null }
    assertNotNull(A.foo())

    A.foo = l@ { return@l nullableUnit() }
    assertNotNull(A.foo())

    return "OK"
}
