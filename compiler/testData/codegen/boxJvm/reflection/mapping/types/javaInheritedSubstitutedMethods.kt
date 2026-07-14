// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public enum J { NORTH, SOUTH, EAST, WEST; }

// FILE: test/I.java
package test;

import java.util.List;

public interface I<T extends I<T>> {
    List<T> f();
}

// FILE: test/C.java
package test;

import java.util.List;

public class C implements I<C> {
    @Override
    public List<C> f() { return null; }
}

// FILE: box.kt
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals
import test.*

fun box(): String {
    val j = J::class.members.single { it.name == "getDeclaringClass" }.returnType.javaType.toString()
    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        // KT-87366
        assertEquals("java.lang.Class<E>", j)
    } else {
        assertEquals("java.lang.Class<test.J>", j)
    }

    val c = C::class.members.single { it.name == "f" }.returnType.javaType.toString()
    assertEquals("java.util.List<test.C>", c)

    return "OK"
}
