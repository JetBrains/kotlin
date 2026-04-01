// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: V.java
public class V {
    public static <T> T first(T... xs) {
        return xs[0];
    }
}

// FILE: Arr.java
public class Arr {
    public static <T> T head(T[] xs) {
        return xs[0];
    }
}

// FILE: main.kt
fun varargExplicitNullable() {
    V.first<String?>("a", null)<!UNSAFE_CALL!>.<!>length
}

fun varargNonNullBaseline() {
    V.first<String>("a", "b").length
}

fun varargInferredNullable(x: String?) {
    V.first(x)<!UNSAFE_CALL!>.<!>length
}

fun arrayExplicitNullable(xs: Array<String?>) {
    Arr.head<String?>(xs)<!UNSAFE_CALL!>.<!>length
}

fun arrayExplicitNonNullBaseline(xs: Array<String>) {
    Arr.head<String>(xs).length
}

fun arrayInferredNullable(xs: Array<String?>) {
    Arr.head(xs)<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, nullableType, stringLiteral */
