// LANGUAGE: +ReportReificationProblemsInDnnAndFlexible
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74516

// FILE: J.java
public class J {
    public static <T> T identity(T t) {
        return t;
    }
}

// FILE: test.kt
inline fun <reified R> inline(r: R, any: Any): R? {
    if (any is R) return any
    return null
}

fun <T> foo(t: T): T? {
    return <!TYPE_PARAMETER_AS_REIFIED!>inline<!>(J.identity(t), "")
}

fun main() {
    foo(1)?.toFloat() // CCE if no error
}

/* GENERATED_FIR_TAGS: dnnType, flexibleType, functionDeclaration, ifExpression, inline, integerLiteral, isExpression,
javaFunction, nullableType, reified, safeCall, smartcast, stringLiteral, typeParameter */
