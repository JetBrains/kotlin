// RUN_PIPELINE_TILL: BACKEND
class My<T: Any>(val y: T?) {

    fun get(): T = run {
        val x = y
        if (x == null) throw Exception()
        <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, equalityExpression, functionDeclaration, ifExpression, lambdaLiteral,
localProperty, nullableType, primaryConstructor, propertyDeclaration, smartcast, typeConstraint, typeParameter */
