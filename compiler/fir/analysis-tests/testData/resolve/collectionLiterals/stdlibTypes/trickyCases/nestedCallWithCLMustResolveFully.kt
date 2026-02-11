// LANGUAGE: +CollectionLiterals
// ISSUE: KT-84145
// RUN_PIPELINE_TILL: FRONTEND

fun <T, R> myRun(x: T, y: T, b: (T) -> R): R = TODO()
fun <T : Set<*>, R> dubRun(x: T, y: T, b: (T) -> R): R = TODO()

fun foo(x: Int) {}
fun foo(x: String) {}

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(myRun([1, 2, 3], setOf(4, 5, 6)) { "" })
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(dubRun([1, 2, 3], [4, 5, 6]) { 42 })

    // ambiguity
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(dubRun(<!AMBIGUOUS_COLLECTION_LITERAL!>[1, 2, 3]<!>, mutableSetOf(4, 5, 6)) { 42 })
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
nullableType, starProjection, stringLiteral, typeConstraint, typeParameter */
