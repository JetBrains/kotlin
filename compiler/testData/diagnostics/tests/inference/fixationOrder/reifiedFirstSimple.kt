// RUN_PIPELINE_TILL: BACKEND
// DUMP_INFERENCE_LOGS: MARKDOWN
// FIR_DUMP
// ISSUE: KT-86728

// FILE: main.kt

inline fun <reified T1 : Any> decode(): T1? {
    return null
}

fun <T2 : Any> decodeNonReified(): T2? {
    return null
}

fun <R> myRun(x: () -> R): R = TODO()

sealed interface Base
sealed interface Derived : Base

val d: Derived = TODO()

fun main() {
    val x1: Base = decode() ?: d
    val x2: Base = decodeNonReified() ?: d
    val x3: Base = myRun { decode() ?: d }
    val x4: Base = myRun { decodeNonReified() ?: d }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, inline, interfaceDeclaration, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, reified, sealed, typeConstraint, typeParameter */
