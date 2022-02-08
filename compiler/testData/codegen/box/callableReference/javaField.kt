// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JClass.java

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

public class JClass {
    public final Set<String> field;

    public JClass(String... ins) {
        field = new HashSet<String>(Arrays.asList(ins));
    }
}

// FILE: main.kt

fun collect(lst: List<JClass>): String {
    return lst.flatMap(JClass::field).joinToString(separator = "")
}

fun box(): String {
    return collect(listOf(JClass("O"), JClass("K")))
}
