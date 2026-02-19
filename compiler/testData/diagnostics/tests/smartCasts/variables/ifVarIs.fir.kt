// RUN_PIPELINE_TILL: BACKEND
public fun bar(s: String) {
    System.out.println("Length of $s is ${s.length}")
}

public fun foo() {
    var s: Any = "not null"
    if (s is String) 
        bar(s)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, ifExpression, isExpression, javaFunction, javaProperty,
localProperty, propertyDeclaration, smartcast, stringLiteral */
