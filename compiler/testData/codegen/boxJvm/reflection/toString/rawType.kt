// WITH_REFLECT
// TARGET_BACKEND: JVM
// FILE: test/A.java
package test;

import java.io.Serializable;

public interface A<A1, A2 extends Number, A3 extends Comparable<String> & Serializable> {}

// FILE: test/AImpl.java
package test;

public interface AImpl extends A {}

// FILE: main.kt
import kotlin.reflect.full.*
import kotlin.test.assertEquals
import test.*

class TestA : AImpl

fun box(): String {
    assertEquals(
        "test.A<(raw) kotlin.Any!, (raw) kotlin.Number!, (raw) kotlin.Comparable<*>!>",
        TestA::class.allSupertypes.single { it.classifier == A::class }.toString(),
    )
    return "OK"
}
