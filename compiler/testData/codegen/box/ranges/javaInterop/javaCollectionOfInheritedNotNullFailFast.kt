// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// Note: This fails on JVM (non-IR) with "Fail: should throw on get() in loop header". The not-null assertion is not generated when
// assigning to the loop variable. The root cause seems to be that the loop variable is a KtParameter and
// CodegenAnnotatingVisitor/RuntimeAssertionsOnDeclarationBodyChecker do not analyze the need for not-null assertions on KtParameters.

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    // Sanity check to make sure there IS an exception even when not in a for-loop
    try {
        val i = JImpl().listOfNotNull()[0]
        return "Fail: should throw on get()"
    } catch (e: IllegalStateException) {}

    try {
        for (i in JImpl().listOfNotNull()) {
            return "Fail: should throw on get() in loop header"
        }
    }
    catch (e: IllegalStateException) {}
    return "OK"
}

interface J {
    fun listOfNotNull(): List<Int>
}

// FILE: JImpl.java
import java.util.*;

public class JImpl implements J {
    // Type argument (Int) gets @EnhancedNullability because it is not nullable in overridden Kotlin function.
    @Override
    public List<Integer> listOfNotNull() {
        return Collections.singletonList(null);
    }
}
