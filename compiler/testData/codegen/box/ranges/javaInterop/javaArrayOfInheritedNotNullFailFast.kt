// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM
// WITH_RUNTIME

// Note: This fails on JVM (non-IR) with "Fail: should throw on get() in loop header". The not-null assertion is not generated when
// assigning to the loop variable. The root cause seems to be that the loop variable is a KtParameter and
// CodegenAnnotatingVisitor/RuntimeAssertionsOnDeclarationBodyChecker do not analyze the need for not-null assertions on KtParameters.

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    // Sanity check to make sure there IS an exception even when not in a for-loop
    try {
        val i = JImpl().arrayOfNotNull()[0]
        return "Fail: should throw on get()"
    } catch (e: IllegalStateException) {}

    try {
        for (i in JImpl().arrayOfNotNull()) {
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
