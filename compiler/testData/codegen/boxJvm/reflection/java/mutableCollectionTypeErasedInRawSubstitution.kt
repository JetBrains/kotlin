// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/B.java
package test;

public interface B<U> extends A<U> {}

// FILE: test/C.java
package test;

public interface C extends B {}

// FILE: 1.kt
package test

import kotlin.test.assertEquals

interface A<T> {
    fun readonly(l: List<T>): List<T>
    fun mutable(l: MutableList<T>): MutableList<T>
}

fun box(): String {
    assertEquals(
        "fun test.C.readonly(kotlin.collections.List<(raw) kotlin.Any?>): kotlin.collections.List<(raw) kotlin.Any?>",
        C::readonly.toString(),
    )
    assertEquals(
        "fun test.C.mutable(kotlin.collections.MutableList<(raw) kotlin.Any?>): kotlin.collections.MutableList<(raw) kotlin.Any?>",
        C::mutable.toString(),
    )
    return "OK"
}
