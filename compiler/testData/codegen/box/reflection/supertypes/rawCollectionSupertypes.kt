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
    // TODO: the remaining difference is tracked in KT-84382.
    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        assertEquals(
            "[test.AImpl, test.A, kotlin.collections.MutableList<(raw) kotlin.Any?>, kotlin.collections.Collection<(raw) kotlin.Any?>, kotlin.collections.Iterable<(raw) kotlin.Any?>, kotlin.Any]",
            TestA::class.allSupertypes.toString(),
        )
    } else {
        assertEquals(
            "[test.AImpl, test.A, kotlin.collections.List<(raw) kotlin.Any!>, kotlin.collections.Collection<(raw) kotlin.Any?>, kotlin.collections.Iterable<(raw) kotlin.Any?>, kotlin.Any]",
            TestA::class.allSupertypes.toString(),
        )
    }

    return "OK"
}
