// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-81441
open class Box<A>(val value: A)

fun <B> Box<B>.extension(): B {
    return value
}

data object FalseNegative : Box<List<List<List<*>>>>(listOf())

fun main() {
    FalseNegative.extension<Int>() + 1 // CCE: class kotlin.collections.EmptyList cannot be cast to class java.lang.Number
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, data, funWithExtensionReceiver, functionDeclaration,
integerLiteral, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, starProjection, typeParameter */
