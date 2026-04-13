// RUN_PIPELINE_TILL: BACKEND
// KT-287 Infer constructor type arguments

import java.util.*

fun attributes() : Map<String, String> = HashMap() // Should be inferred;
val attributes : Map<String, String> = HashMap() // Should be inferred;

fun foo(m : Map<String, String>) {}

fun test() {
    foo(HashMap())
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, propertyDeclaration */
