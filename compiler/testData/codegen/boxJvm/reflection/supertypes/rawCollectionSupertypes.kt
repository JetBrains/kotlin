// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/A.java
package test;

import java.util.List;

public interface A extends List {}

// FILE: test/AImpl.java
package test;

public interface AImpl extends A {}

// FILE: box.kt
package test

import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

abstract class TestA : AImpl

fun box(): String {
    assertEquals(
        "[test.AImpl, test.A, kotlin.collections.MutableList<(raw) kotlin.Any?>, kotlin.collections.Collection<(raw) kotlin.Any?>, kotlin.collections.Iterable<(raw) kotlin.Any?>, kotlin.Any]",
        TestA::class.allSupertypes.toString(),
    )

    return "OK"
}
