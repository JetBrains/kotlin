// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: A.java
public class A {
    public static <T extends CharSequence> T id(T x) {
        return x;
    }
}

// FILE: B.java
public class B {
    public static <U extends CharSequence, T extends U> T id(T x) {
        return x;
    }
}

// FILE: main.kt
fun oneHopNullable() {
    val x = A.id<String?>("abc")
    x.length
}

fun twoHopNullable() {
    val x = B.id<String?, String?>("abc")
    x.length
}

fun twoHopNonNullBaseline() {
    val x = B.id<String, String>("abc")
    x.length
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, localProperty, nullableType, propertyDeclaration,
stringLiteral */
