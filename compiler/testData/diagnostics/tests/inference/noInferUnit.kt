// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82863
// DIAGNOSTICS: -ERROR_SUPPRESSION

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> fooTakingLambda(fn: () -> T): @kotlin.internal.NoInfer T = fn()

fun bar() {
    val x: Unit = <!CANNOT_INFER_PARAMETER_TYPE!>fooTakingLambda<!> { <!RETURN_TYPE_MISMATCH!>42<!> }
}

fun baz() {
    val x: Unit = fooTakingLambda { }
}

fun bazz() {
    val x: Int = fooTakingLambda { 42 }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
