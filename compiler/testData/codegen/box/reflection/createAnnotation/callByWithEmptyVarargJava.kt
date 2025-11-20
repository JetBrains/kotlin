// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J1.java
public @interface J1 {
    String[] value();
}

// FILE: J2.java
public @interface J2 {
    String[] value();
    int[] notValue();
}

// FILE: J3.java
public @interface J3 {
    String[] notValue();
}

// FILE: box.kt
import kotlin.reflect.full.primaryConstructor
import kotlin.test.*

fun box(): String {
    // Java annotation constructor parameter is vararg if and only if it's a single parameter named "value".
    // In this case, kotlin-reflect should treat this parameter as optional (modulo KT-82881) and allow its absence in `callBy`.

    val j1 = J1::class.primaryConstructor!!
    assertFalse(j1.parameters.single().isOptional)
    assertEquals(emptyList(), j1.callBy(emptyMap()).value.toList())

    val j2 = J2::class.primaryConstructor!!
    assertEquals(listOf(false, false), j2.parameters.map { it.isOptional })
    assertFailsWith<IllegalArgumentException> { j2.callBy(emptyMap()) }

    val j3 = J3::class.primaryConstructor!!
    assertFalse(j3.parameters.single().isOptional)
    assertFailsWith<IllegalArgumentException> { j3.callBy(emptyMap()) }

    return "OK"
}
