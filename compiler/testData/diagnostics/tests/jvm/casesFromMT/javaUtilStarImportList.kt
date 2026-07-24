// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FILE: J.java
import java.util.*;

public class J {
    public static List<String> foo() {
        return Collections.singletonList("ok");
    }
    public static Map.Entry<String, Integer> firstEntry(Map<String, Integer> m) {
        return m.entrySet().iterator().next();
    }
}

// FILE: useSite.kt
// Force Kotlin to bind the return type and read its type argument.
fun bar(): String {
    val list: List<String> = J.foo()
    val entry: Map.Entry<String, Int> = J.firstEntry(java.util.HashMap()) ?: error("no entry")
    return list.first() + " " + entry.key
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, lambdaLiteral, stringLiteral */
