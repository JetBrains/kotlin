// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63914

// FILE: J.java

import java.util.*;

public interface J<T extends Number> {
    T foo(List<T> p0, Map<T, ? extends Set<T>> p1);
}

// FILE: K.kt

import kotlin.test.assertEquals

object O : J<Long> {
    override fun foo(p0: List<Long>, p1: Map<Long, out Set<Long>>): Long = 42L
}

class A : J<Long> by O

fun box(): String {
    val m = A::class.java.getDeclaredMethod("foo", List::class.java, Map::class.java)
    assertEquals(
        "[interface java.util.List, interface java.util.Map]",
        m.parameterTypes.contentToString()
    )
    assertEquals(
        "[java.util.List<java.lang.Long>, java.util.Map<java.lang.Long, ? extends java.util.Set<java.lang.Long>>]",
        m.genericParameterTypes.contentToString()
    )
    return "OK"
}
