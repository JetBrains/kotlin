// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// Note: This fails on JVM (non-IR) with "Fail: should throw on get()". The not-null assertion is not generated when assigning to the
// variables in the destructuring declaration. The root cause seems to be that
// CodegenAnnotatingVisitor/RuntimeAssertionsOnDeclarationBodyChecker do not analyze the need for not-null assertions on
// KtDestructuringDeclarations and their entries.

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    // Sanity check to make sure there IS an exception even when not in a for-loop
    try {
        val (index, i) = JImpl().arrayOfNotNull().withIndex().first()
        return "Fail: should throw on get()"
    } catch (e: IllegalStateException) {}

    try {
        for ((index, i) in JImpl().arrayOfNotNull().withIndex()) {
            return "Fail: should throw on get() in loop header"
        }
    }
    catch (e: IllegalStateException) {}
    return "OK"
}

interface J {
    fun arrayOfNotNull(): Array<Int>
}

// FILE: JImpl.java
public class JImpl implements J {
    // The only way to get @EnhancedNullability on the array element type (Int) is to override a Kotlin function that
    // returns `Array<Int>` (where Int is not nullable). `@NotNull Integer[]` makes the array not nullable, not String.
    @Override
    public Integer[] arrayOfNotNull() {
        return new Integer[] { null };
    }
}
