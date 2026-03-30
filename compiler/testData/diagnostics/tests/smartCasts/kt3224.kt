// RUN_PIPELINE_TILL: BACKEND
// Works already in M11

fun test(c : Class<*>) {
    val sc = c <!UNCHECKED_CAST!>as Class<String><!>
    // No ambiguous overload
    c.getAnnotations();
    sc.getAnnotations();
}

/* GENERATED_FIR_TAGS: asExpression, flexibleType, functionDeclaration, localProperty, propertyDeclaration, smartcast,
starProjection */
