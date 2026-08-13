// TARGET_BACKEND: JVM
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

// FILE: box.kt
fun box(): String {
    val actual = mutableListOf<Int>()
    for (x: Int in J.platformList().asSequence()) {
        actual += x
    }
    if (actual != listOf(1, 2, 3)) return "Fail: $actual"
    return "OK"
}

// FILE: J.java
import java.util.*;

public class J {
    public static List<Integer> platformList() {
        return Arrays.asList(1, 2, 3);
    }
}
