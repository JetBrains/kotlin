// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

package aaa

import checkSubtype

infix fun <T> T.foo(t: T) = t

fun <T> id(t: T) = t

fun a() {
    val i = id(2 foo 3)
    checkSubtype<Int>(i) // i shouldn't be resolved to error element
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
