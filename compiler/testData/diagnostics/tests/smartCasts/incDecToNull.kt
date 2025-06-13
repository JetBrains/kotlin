// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class IncDec {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): Unit {}
}

fun foo(): IncDec {
    var x = IncDec()
    x = x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>
    x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>
    return x
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, incrementDecrementExpression, intersectionType,
localProperty, operator, propertyDeclaration, smartcast */
