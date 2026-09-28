// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-89232
// FIR_DUMP

enum class MyEnum { X }

sealed class MySealed {
    object A : MySealed()
}

fun enumEntry(i: Int): MyEnum = MyEnum.X
fun sealedInheritor(i: Int): MySealed = MySealed.A

fun <T, R> viaLambdaWithParameter(x: T, y: R, f: (R) -> T) {}
fun <T> viaLambdaResult(x: T, f: () -> T) {}
fun <T, R> viaCallableReference(x: T, f: (R) -> T) {}
fun <T, R> viaCallableReferenceWithKnownParameter(x: T, r: R, f: (R) -> T) {}

fun <T> noBounds(x: T) {}
fun <T, R> unrelatedLambda(x: T, f: (R) -> Unit) {}

fun testEnum() {
    viaLambdaWithParameter(X, 1) { MyEnum.X }
    viaLambdaResult(X) { MyEnum.X }
    viaCallableReference(X, ::enumEntry)
    viaCallableReferenceWithKnownParameter(X, 1, ::enumEntry)

    <!CANNOT_INFER_PARAMETER_TYPE!>noBounds<!>(<!UNRESOLVED_REFERENCE!>X<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>unrelatedLambda<!>(<!UNRESOLVED_REFERENCE!>X<!>) { r: Int -> }
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>unrelatedLambda<!>(<!UNRESOLVED_REFERENCE!>X<!>) <!CANNOT_INFER_PARAMETER_TYPE!>{ }<!>
}

fun testSealed() {
    viaLambdaWithParameter(A, 1) { MySealed.A }
    viaLambdaResult(A) { MySealed.A }
    viaCallableReference(A, ::sealedInheritor)
    viaCallableReferenceWithKnownParameter(A, 1, ::sealedInheritor)

    <!CANNOT_INFER_PARAMETER_TYPE!>noBounds<!>(<!UNRESOLVED_REFERENCE!>A<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>unrelatedLambda<!>(<!UNRESOLVED_REFERENCE!>A<!>) { r: Int -> }
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>unrelatedLambda<!>(<!UNRESOLVED_REFERENCE!>A<!>) <!CANNOT_INFER_PARAMETER_TYPE!>{ }<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, enumDeclaration, enumEntry, functionDeclaration,
functionalType, integerLiteral, lambdaLiteral, nestedClass, nullableType, objectDeclaration, sealed, typeParameter */
