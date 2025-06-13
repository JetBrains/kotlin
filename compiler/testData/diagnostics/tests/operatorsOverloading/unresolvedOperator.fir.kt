// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

fun test() {
    var a = ""

    a<!UNRESOLVED_REFERENCE!>++<!>
    a<!UNRESOLVED_REFERENCE!>--<!>
    <!UNRESOLVED_REFERENCE!>+<!>a
    <!UNRESOLVED_REFERENCE!>-<!>a
    <!UNRESOLVED_REFERENCE!>!<!>a
    a <!UNRESOLVED_REFERENCE!>*<!> a
    true <!UNRESOLVED_REFERENCE!>+<!> false
    a <!UNRESOLVED_REFERENCE!>-<!> a
    a <!UNRESOLVED_REFERENCE!>/<!> a
    a <!UNRESOLVED_REFERENCE!>%<!> a
    a <!UNRESOLVED_REFERENCE!>..<!> a
    a <!UNRESOLVED_REFERENCE!>..<<!> a
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, incrementDecrementExpression, localProperty,
multiplicativeExpression, propertyDeclaration, rangeExpression, stringLiteral, unaryExpression */
