// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public enum J {
    ENTRY;

    public String getName() {
        return "OK";
    }

    public int getOrdinal() {
        return 42;
    }
}

// FILE: box.kt
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.test.assertEquals

fun box(): String {
    val members = J::class.members

    val getName = members.single { it.name == "getName" } as KFunction<*>
    assertEquals("OK", getName.call(J.ENTRY))

    val getOrdinal = members.single { it.name == "getOrdinal" } as KFunction<*>
    assertEquals(42, getOrdinal.call(J.ENTRY))

    val name = members.single { it.name == "name" } as KProperty1<*, *>
    assertEquals("ENTRY", name.call(J.ENTRY))

    val ordinal = members.single { it.name == "ordinal" } as KProperty1<*, *>
    assertEquals(0, ordinal.call(J.ENTRY))

    return "OK"
}
