// RUN_PIPELINE_TILL: BACKEND
public fun fooNotNull(s: String) {
    System.out.println("Length of $s is ${s.length}")
}

public fun foo() {
    var s: String? = "not null"
    if (s != null)
        fooNotNull(s)
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, ifExpression, javaFunction, javaProperty,
localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral */
