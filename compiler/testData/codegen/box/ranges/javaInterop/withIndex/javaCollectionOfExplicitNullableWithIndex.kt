// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// JVM_TARGET: 1.8

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    val actualIndices = mutableListOf<Int>()
    val actualValues = mutableListOf<Int?>()
    for ((index, i) in J.listOfNullable().withIndex()) {
        actualIndices += index
        actualValues += i
    }
    assertEquals(listOf(0, 1), actualIndices)
    assertEquals(listOf(42, null), actualValues)
    return "OK"
}

// FILE: J.java
import java.util.*;
import org.jetbrains.annotations.*;

public class J {
    public static List<@Nullable Integer> listOfNullable() {
        List<Integer> list = new ArrayList<>();
        list.add(42);
        list.add(null);
        return list;
    }
}

// FILE: Nullable.java
package org.jetbrains.annotations;

import java.lang.annotation.*;

// org.jetbrains.annotations used in the compiler is version 13, whose @Nullable does not support the TYPE_USE target (version 15 does).
// We're using our own @org.jetbrains.annotations.Nullable for testing purposes.
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
public @interface Nullable {
}
