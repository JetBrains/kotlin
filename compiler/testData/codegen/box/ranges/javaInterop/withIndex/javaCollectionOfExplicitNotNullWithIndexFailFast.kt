// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// JVM_TARGET: 1.8

// Note: This fails on JVM (non-IR) with "Fail: should throw on get()". The not-null assertion is not generated when assigning to the
// variables in the destructuring declaration. The root cause seems to be that
// CodegenAnnotatingVisitor/RuntimeAssertionsOnDeclarationBodyChecker do not analyze the need for not-null assertions on
// KtDestructuringDeclarations and their entries.

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    // Sanity check to make sure there IS an exception even when not in a for-loop
    try {
        val (index, i) = J.listOfNotNull().withIndex().first()
        return "Fail: should throw on get()"
    } catch (e: IllegalStateException) {}

    try {
        for ((index, i) in J.listOfNotNull().withIndex()) {
            return "Fail: should throw on get() in loop header"
        }
    }
    catch (e: IllegalStateException) {}
    return "OK"
}

// FILE: J.java
import java.util.*;
import org.jetbrains.annotations.*;

public class J {
    public static List<@NotNull Integer> listOfNotNull() {
        return Collections.singletonList(null);
    }
}

// FILE: NotNull.java
package org.jetbrains.annotations;

import java.lang.annotation.*;

// org.jetbrains.annotations used in the compiler is version 13, whose @NotNull does not support the TYPE_USE target (version 15 does).
// We're using our own @org.jetbrains.annotations.NotNull for testing purposes.
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
public @interface NotNull {
}
