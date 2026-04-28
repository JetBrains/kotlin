// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FILE: J.java

public class J {
    public static java.util.Map.Entry<String, Integer> first(java.util.Map<String, Integer> m) {
        return m.entrySet().iterator().next();
    }
}

// FILE: useSite.kt
fun bar(m: java.util.HashMap<String, Int>): String {
    val entry = J.first(m)
    return "${entry.key}=${entry.value}"
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, stringTemplate */
