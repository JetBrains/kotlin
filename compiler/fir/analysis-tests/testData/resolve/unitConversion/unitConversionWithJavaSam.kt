// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// ISSUE: KT-84393
// DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: J.java
public interface J {
    void run();
}

// FILE: test.kt
fun useJavaSam(r: J) {}

val intLambda: () -> Int = { 42 }

fun test() {
    useJavaSam(intLambda)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, javaType, lambdaLiteral, propertyDeclaration,
samConversion */
