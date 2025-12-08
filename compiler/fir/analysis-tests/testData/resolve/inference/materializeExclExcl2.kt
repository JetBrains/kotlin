// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81948
// DIAGNOSTICS: -UNCHECKED_CAST
// FIR_DUMP

fun <R> myRun(x: () -> R): R = x()

fun <T> materialize(): T? = "" as T?

val x = <!CANNOT_INFER_PARAMETER_TYPE!>myRun<!> { <!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>() }!!

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, functionDeclaration, functionalType, lambdaLiteral, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
