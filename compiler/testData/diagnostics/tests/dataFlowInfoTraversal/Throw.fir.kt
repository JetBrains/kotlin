// RUN_PIPELINE_TILL: FRONTEND
fun bar(x: Int): RuntimeException = RuntimeException(x.toString())

fun foo() {
    val x: Int? = null

    if (x == null) throw bar(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    throw bar(x)
    throw bar(x)
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast */
