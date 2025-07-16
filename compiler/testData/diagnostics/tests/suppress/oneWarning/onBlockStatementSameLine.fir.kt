// RUN_PIPELINE_TILL: FRONTEND

fun <T : CharSequence> foo(x: Array<Any>, block: (T, Int) -> Int) {
    var r: Any?

    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!><!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>@Suppress("UNCHECKED_CAST") r<!> = block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int)<!>

    // to prevent unused assignment diagnostic for the above statement
    r.hashCode()

    var i = 1

    if (i != 1) {
        <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Suppress("UNCHECKED_CAST") i <!UNRESOLVED_REFERENCE!>+=<!> block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!><!>
    }

    if (i != 1) @Suppress("UNCHECKED_CAST")
    i += block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>

    if (i != 1) <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Suppress("UNCHECKED_CAST") i <!UNRESOLVED_REFERENCE!>+=<!> block(x[0] as T, "" <!CAST_NEVER_SUCCEEDS!>as<!> Int).<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!><!>
}

/* GENERATED_FIR_TAGS: additiveExpression, asExpression, assignment, equalityExpression, functionDeclaration,
functionalType, ifExpression, integerLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral,
typeConstraint, typeParameter */
