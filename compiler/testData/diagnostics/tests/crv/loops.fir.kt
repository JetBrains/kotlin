// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun getAny(): Any = true
fun getBoolean(): Boolean = true

fun whileLoop() {
    var cur = 10
    while (cur >= 0) {
        cur--
    }

    while (getAny() as Boolean) {
        cur--
    }

    while(getBoolean()) {
        cur--
    }
}

fun inOperator(c: Char, vararg cs: Char) {
    var cur = 10
    // TODO: annotate .contains in stdlib
//    c in cs // unused
    val z = c in cs // used
    do {
        cur--
    } while (cur >= 0 && c in cs)
}

fun forLoop() {
    val cs = listOf('a', 'b', 'c')
    for (c in cs) {
        <!UNUSED_EXPRESSION!>c<!> // unused, but OK because it is local
    }
    for (i in 1..10) {
        i <!RETURN_VALUE_NOT_USED!>+<!> 1
    }
}

var nonLocal: Int = 0

fun operators() {
    nonLocal++ // unused, but discardable
    --nonLocal // unused, but discardable
    <!RETURN_VALUE_NOT_USED!>-<!>nonLocal // unary minus — unused, should be reported
    nonLocal <!RETURN_VALUE_NOT_USED!>+<!> nonLocal // unused, should be reported
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, annotationUseSiteTargetFile, asExpression, assignment,
comparisonExpression, doWhileLoop, forLoop, functionDeclaration, incrementDecrementExpression, integerLiteral,
localProperty, propertyDeclaration, rangeExpression, unaryExpression, vararg, whileLoop */
