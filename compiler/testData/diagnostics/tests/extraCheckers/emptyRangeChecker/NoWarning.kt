// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun foo() {
    for (i in 1..2) { }

    val <!UNUSED_VARIABLE!>a<!> = 3..4

    val v = 1
    if (v in 5..6) { }

    if (' ' in 'd'..'d') { }

    if (0.0F in 0.01F..0.02F) { }

    if (0.0 in 0.01..0.02) { }
}


fun backward() {
    for (i in 2 downTo 1) { }

    val <!UNUSED_VARIABLE!>a<!> = 4 downTo 3

    val v = 1
    if (v in -5 downTo -6) { }

    if (' ' in 'c' downTo 'c') { }
}

fun until() {
    for (i in 1 until 2) { }

    val <!UNUSED_VARIABLE!>a<!> = 3 until 4

    val v = 1
    if (v in -5 until -4) { }

    if (' ' in '7' until '9') { }
}

fun rangeUntil() {
    for (i in 0..<1) {}

    if (' ' in '8'..<'9') { }

    if (0.0F in 0.0F..<1.0F) { }

    if (0.0 in 0.0..<1.0) { }
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, ifExpression, integerLiteral, localProperty, propertyDeclaration,
rangeExpression */
