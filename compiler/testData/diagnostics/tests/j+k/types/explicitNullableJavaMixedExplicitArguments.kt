// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: J.java
public class J {
    public static <K, V> Pair<K, V> pair(K first, V second) {
        return new Pair<>(first, second);
    }

    public static class Pair<K, V> {
        public final K first;
        public final V second;

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }
    }
}

// FILE: main.kt
fun nullableKeyNonNullValue() {
    val p = J.pair<String?, String>("a", "b")
    p.first.length
    p.second.length
}

fun nonNullKeyNullableValue() {
    val p = J.pair<String, String?>("a", "b")
    p.first.length
    p.second.length
}

fun bothNullable() {
    val p = J.pair<String?, String?>("a", "b")
    p.first.length
    p.second.length
}

fun bothNonNullBaseline() {
    val p = J.pair<String, String>("a", "b")
    p.first.length
    p.second.length
}

fun inferredNullableBaseline(x: String?) {
    val p = J.pair(x, "b")
    p.first<!UNSAFE_CALL!>.<!>length
    p.second.length
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, nullableType, propertyDeclaration,
stringLiteral */
