// RUN_PIPELINE_TILL: BACKEND
data class MyClass(val x: String?)

fun foo(y: MyClass): Int {
    val z = y.x?.subSequence(0, <!DEBUG_INFO_SMARTCAST!>y.x<!>.length)
    return z?.length ?: -1
}

/* GENERATED_FIR_TAGS: classDeclaration, data, elvisExpression, functionDeclaration, integerLiteral, localProperty,
nullableType, primaryConstructor, propertyDeclaration, safeCall, smartcast */
