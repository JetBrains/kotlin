// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// LANGUAGE: +DataFlowBasedExhaustiveness

// FILE: Kotlin.kt
fun foo(b: Boolean): Int {
    if (b == false) return 1
    return when (b) {
        true -> 2
    }
}

fun bar(): Int {
    val v = Java.getValue()
    if (v == true) return 1
    return when (v) {
        false -> 2
    }
}

fun qux(b: Boolean?): Int {
    if ((b == true) == false) return 1
    return when (b) {
        true -> 2
    }
}

fun quux(b: Boolean?): Int {
    if ((b == true) == true) return 1
    return when (b) {
        null -> 2
        false -> 3
    }
}

// FILE: Java.java
class Java {
    public static boolean getValue() { return false; }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, javaFunction,
localProperty, nullableType, propertyDeclaration, smartcast, whenExpression, whenWithSubject */
