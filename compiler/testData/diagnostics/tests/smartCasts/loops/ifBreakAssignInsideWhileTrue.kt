// RUN_PIPELINE_TILL: FRONTEND
public fun foo(x: String?): Int {
    var y: Any
    while (true) {
        y = if (x == null) break else <!DEBUG_INFO_SMARTCAST!>x<!>
    }
    // In future we can infer this initialization
    <!UNINITIALIZED_VARIABLE!>y<!>.hashCode()
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: assignment, break, equalityExpression, functionDeclaration, ifExpression, localProperty,
nullableType, propertyDeclaration, smartcast, whileLoop */
