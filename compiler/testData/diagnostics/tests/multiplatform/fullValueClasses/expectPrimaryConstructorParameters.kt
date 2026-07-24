// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +FullValueClasses

// MODULE: m1-common
// FILE: common.kt
expect value class Final(val value: String = "")
expect value class Final1(val value1: String = "", val value2: String = "")
expect abstract value class Abstract(value: String = "")
expect sealed value class Sealed(value: String = "")


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual value class Final actual constructor(val value: String)
actual value class Final1 actual constructor(val value1: String, val value2: String)
actual abstract value class Abstract actual constructor(value: String)
actual sealed value class Sealed actual constructor(value: String)

sealed class A : Sealed()

fun test() {
    Final() // KT-60476 Fixed in K2
    object : Abstract() {}
}

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, classDeclaration, expect, functionDeclaration, integerLiteral,
primaryConstructor, propertyDeclaration, stringLiteral, value */
