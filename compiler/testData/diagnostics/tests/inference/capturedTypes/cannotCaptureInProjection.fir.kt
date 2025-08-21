// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<in Int>) {
    val r: Array<in Int?> = bar(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<in Int>) {
    val r: Array<out Array<in Int?>> = foo(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
inProjection, infix, localProperty, nullableType, outProjection, propertyDeclaration, typeConstraint, typeParameter,
typeWithExtension */
