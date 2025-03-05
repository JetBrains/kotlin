// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun getAny(): Any = true

fun whileLoop() {
    var cur = 10
    while (cur >= 0) {
        cur--
    }

    while (getAny() as Boolean) {
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
        <!RETURN_VALUE_NOT_USED!>i + 1<!>
    }
}

var nonLocal: Int = 0

fun operators() {
    nonLocal++ // unused, but discardable
    --nonLocal // unused, but discardable
    <!RETURN_VALUE_NOT_USED!>-nonLocal<!> // unary minus — unused, should be reported
    <!RETURN_VALUE_NOT_USED!>nonLocal + nonLocal<!> // unused, should be reported
}
