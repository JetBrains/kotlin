// RUN_PIPELINE_TILL: FRONTEND
//For testing error messages text see DiagnosticMessageTest.testFunctionPlaceholder
package a

class A<T, R>
fun <T, R> foo(a: A<T, R>) = a
fun <T, R> bar(f: (T) -> R) = f

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>foo<!> <!ARGUMENT_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!> }<!>
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>foo<!> <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> x}<!>
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>foo<!> <!ARGUMENT_TYPE_MISMATCH!>{ x: Int -> x}<!>

    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>bar<!> <!CANNOT_INFER_PARAMETER_TYPE!>{ it + 1 }<!>
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>bar<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> x + 1}
    bar { x: Int -> x + 1}
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, nullableType, typeParameter */
