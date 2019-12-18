// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// Note: This fails on JVM (non-IR) with a NullPointerException in the loop header. The not-null assertion is not generated when
// assigning to the loop variable. The root cause seems to be that the loop variable is a KtParameter and
// CodegenAnnotatingVisitor/RuntimeAssertionsOnDeclarationBodyChecker do not analyze the need for not-null assertions on KtParameters.
// The NPE is due to calling `intValue()` on the null Int; it is expected to be asserted as non-null first.

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    // Sanity check to make sure there IS an exception even when not in a for-loop
    try {
        val i: Int = J.arrayOfMaybeNullable()[0]
        return "Fail: should throw on get()"
    } catch (e: IllegalStateException) {}

    try {
        for (i: Int in J.arrayOfMaybeNullable()) {
            return "Fail: should throw on get() in loop header"
        }
    }
    catch (e: IllegalStateException) {}
    return "OK"
}

// FILE: J.java
public class J {
    public static Integer[] arrayOfMaybeNullable() {
        return new Integer[] { null };
    }
}
