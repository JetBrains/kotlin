// RUN_PIPELINE_TILL: FRONTEND

class IncDec {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): Unit {}
}

fun foo(): IncDec {
    var x = IncDec()
    x = x<!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>++<!>
    x<!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>++<!>
    return x
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, incrementDecrementExpression, intersectionType,
localProperty, operator, propertyDeclaration, smartcast */
