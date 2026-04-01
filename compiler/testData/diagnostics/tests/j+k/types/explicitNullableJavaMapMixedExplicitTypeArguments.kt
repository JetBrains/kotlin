// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: JavaMaps.java
import java.util.Map;

public class JavaMaps {
    public static <K, V> Map<K, V> id(Map<K, V> xs) { return xs; }
    public static <K, V> V get(Map<K, V> xs, K key) { return xs.get(key); }
}

// FILE: Test.kt
fun takeString(x: String) {}
fun takeNullableString(x: String?) {}

fun test(m1: Map<String, String?>, m2: Map<String, String>) {
    val a: Map<String, String?> = JavaMaps.id<String, String?>(m1)
    val b: Map<String, String> = JavaMaps.id<String, String>(m2)

    val v1: String? = JavaMaps.get<String, String?>(m1, "k")
    val v2: String = JavaMaps.get<String, String>(m2, "k")

    takeNullableString(JavaMaps.get<String, String?>(m1, "k"))
    takeString(JavaMaps.get<String, String?>(m1, "k"))

    val badValue: String = JavaMaps.get<String, String?>(m1, "k")
    val badMap: Map<String, String> = JavaMaps.id<String, String?>(m1)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, localProperty,
nullableType, propertyDeclaration, stringLiteral */