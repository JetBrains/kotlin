// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.*

class A {
    val x = 1
    fun x() {}
}

fun f1(): KProperty<Int> = A::x  // ok, property
fun f2(): (A) -> Unit = A::x     // ok, function

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, integerLiteral,
propertyDeclaration */
