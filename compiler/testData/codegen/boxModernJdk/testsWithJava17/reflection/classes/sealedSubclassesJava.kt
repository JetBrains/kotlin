// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;
public sealed class J permits J1, J2 {}

// FILE: test/J1.java
package test;
public sealed class J1 extends J permits J1Impl {}

// FILE: test/J2.java
package test;
public final class J2 extends J {}

// FILE: test/J1Impl.java
package test;
public final class J1Impl extends J1 {}

// FILE: box.kt
package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun sealedSubclassNames(c: KClass<*>) = c.sealedSubclasses.map { it.simpleName ?: throw AssertionError("Unnamed class: ${it.java}") }.sorted()

fun box(): String {
    assertEquals(listOf("J1", "J2"), sealedSubclassNames(J::class))
    assertEquals(listOf("J1Impl"), sealedSubclassNames(J1::class))
    assertEquals(emptyList(), sealedSubclassNames(J2::class))

    return "OK"
}
