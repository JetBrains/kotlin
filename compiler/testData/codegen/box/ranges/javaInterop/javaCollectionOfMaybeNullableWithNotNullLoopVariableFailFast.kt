// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    // Sanity check to make sure there IS an exception even when not in a for-loop
    try {
        val i: Int = J.listOfMaybeNullable()[0]
        return "Fail: should throw on get()"
    } catch (e: NullPointerException) {}

    try {
        for (i: Int in J.listOfMaybeNullable()) {
            return "Fail: should throw on get() in loop header"
        }
    }
    catch (e: NullPointerException) {}
    return "OK"
}

// FILE: J.java
import java.util.*;

public class J {
    public static List<Integer> listOfMaybeNullable() {
        return Collections.singletonList(null);
    }
}
