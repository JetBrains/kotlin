// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    val actualValues = mutableListOf<Int>()
    for (i in JImpl().listOfNotNull()) {
        actualValues += i
    }
    assertEquals(listOf(42, -42), actualValues)
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
        List<Integer> list = new ArrayList<Integer>();
        list.add(42);
        list.add(-42);
        return list;
    }
}
