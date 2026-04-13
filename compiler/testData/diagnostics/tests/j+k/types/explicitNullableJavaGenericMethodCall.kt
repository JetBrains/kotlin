// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: J.java
public class J {
    public static <T> T id(T x) {
        return x;
    }
}

// FILE: main.kt
fun explicitNullableReceiver() {
    J.id<String?>("abc")<!UNSAFE_CALL!>.<!>length
}

fun inferredNullableReceiver(x: String?) {
    J.id(x)<!UNSAFE_CALL!>.<!>length
}

fun explicitNonNullBaseline() {
    J.id<String>("abc").length
}

fun nullableSmartCastAfterCall(x: String?) {
    val y = J.id<String?>(x)
    if (y != null) {
        y.length
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, javaFunction, localProperty, nullableType, propertyDeclaration,
smartcast, stringLiteral */
