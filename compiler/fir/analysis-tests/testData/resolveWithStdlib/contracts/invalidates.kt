// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +ImprovedAliasTracking

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
inline fun <A, B> A.uses(block: (A) -> B): B {
    contract {
        invalidates(this@uses)
    }
    return block(this@uses)
}

data class A(val n: Int)

fun A.foo() { }

fun test1() {
    val x = A(1)
    val t = x.uses { it.n }
    val g = <!INVALIDATED_REFERENCE("INVALIDATED")!>x<!>.n
    val h = <!INVALIDATED_REFERENCE("INVALIDATED")!>x<!>
    <!INVALIDATED_REFERENCE("INVALIDATED"), TWO_REFERENCES("[x, h]")!>x<!>.foo()
}

fun test2() {
    val x = A(1)
    val y = x
    val t = <!TWO_REFERENCES("[x, y]")!>x<!>.uses { it.n }
    <!INVALIDATED_REFERENCE("INVALIDATED")!>y<!>.foo()
}

data class B(var n: Int)

fun test3() {
    val x = B(1)
    val y = x
    <!TWO_REFERENCES("[x, y]")!>y<!>.n = 2
}

fun test4() {
    val x = B(1)
    val (n) = x
    x.n = 2
}

fun test5() {
    val x = B(1)
    val y = x
    val z = <!TWO_REFERENCES("[x, y]")!>y<!>
    <!MULTIPLE_REFERENCES("[x, y, z]")!>x<!>.n = 2
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, contracts, funWithExtensionReceiver, functionDeclaration,
functionalType, inline, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
thisExpression, typeParameter */
