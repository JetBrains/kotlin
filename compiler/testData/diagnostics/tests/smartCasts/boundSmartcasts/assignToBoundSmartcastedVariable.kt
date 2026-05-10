// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-53752
// INFERENCE_HELPERS

interface A
interface B {
    fun fooB(x: Int): String
}

class Foo

fun test(ab: A) {
    if (ab is B) {
        var z = id(ab) // materialize smartcast
        z <!ASSIGNMENT_TYPE_MISMATCH!>=<!> Foo() // unsafe assignment
        z.fooB(1)
    }
}

/* GENERATED_FIR_TAGS: assignment, capturedType, checkNotNullCall, classDeclaration, functionDeclaration, ifExpression,
integerLiteral, interfaceDeclaration, intersectionType, isExpression, localProperty, nullableType, outProjection,
propertyDeclaration, smartcast, typeParameter, vararg */
