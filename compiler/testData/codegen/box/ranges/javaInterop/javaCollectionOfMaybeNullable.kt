// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    val actualValues = mutableListOf<Int>()
    for (i in J.listOfMaybeNullable()) {
        actualValues += i
    }
    assertEquals(listOf(42, null), actualValues)
    return "OK"
}

// FILE: J.java
import java.util.*;

public class J {
    public static List<Integer> listOfMaybeNullable() {
        List<Integer> list = new ArrayList<>();
        list.add(42);
        list.add(null);
        return list;
    }
}
