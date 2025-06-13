// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_VARIABLE

fun <T> foo(l: MutableList<T>): MutableList<T> = l
fun test(l: MutableList<out Int>) {
    val a: MutableList<out Int> = foo(l)
    val b = foo(l)
    b checkType { _< MutableList<out Int> >() }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, lambdaLiteral, localProperty, nullableType, outProjection, propertyDeclaration, typeParameter, typeWithExtension */
