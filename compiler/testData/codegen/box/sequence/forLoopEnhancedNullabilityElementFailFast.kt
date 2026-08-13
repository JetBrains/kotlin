// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

// FILE: box.kt
fun box(): String {
    var sum = 0
    try {
        for (x in J.listOfNotNull().asSequence()) {
            sum += x
        }
    } catch (e: NullPointerException) {
        return "OK"
    }
    return "Fail: expected NPE, got sum = $sum"
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
