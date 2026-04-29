// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CallCompletionRefinementsFor25
// ISSUE: KT-86042

fun <R1, I, R2> foo(r1: R1, i: I, y: (I) -> R1, z: (I) -> R2): R2 = TODO()

fun bar(x: Any) {}

interface Base
interface Derived : Base

fun test(base: Base, derived: Derived) {
    foo(
        derived, "",
        { base },
        { "" }
    )

    bar(
        foo(
            derived, "",
            { <!RETURN_TYPE_MISMATCH!>base<!> },
            { "" }
        )
    )
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, nullableType,
stringLiteral, typeParameter */
