// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun foo() {
    for (i in <!EMPTY_RANGE!>2..1<!>) { }

    val <!UNUSED_VARIABLE!>a<!> = <!EMPTY_RANGE!>10..0<!>

    val v = 1
    if (v in <!EMPTY_RANGE!>10..1<!>) { }

    if (' ' in <!EMPTY_RANGE!>'b'..'a'<!>) { }

    if (0.0F in <!EMPTY_RANGE!>0.02F..0.01F<!>) { }

    if (0.0 in <!EMPTY_RANGE!>0.02..0.01<!>) { }
}

fun backward() {
    for (i in <!EMPTY_RANGE!>1 downTo 2<!>) { }

    val <!UNUSED_VARIABLE!>a<!> = <!EMPTY_RANGE!>-3 downTo 4<!>

    val v = 1
    if (v in <!EMPTY_RANGE!>0 downTo 6<!>) { }

    if (' ' in <!EMPTY_RANGE!>'a' downTo 'b'<!>) { }
}

fun until() {
    for (i in <!EMPTY_RANGE!>1 until 1<!>) { }

    val <!UNUSED_VARIABLE!>a<!> = <!EMPTY_RANGE!>4 until 3<!>

    val v = 1
    if (v in <!EMPTY_RANGE!>-5 until -5<!>) { }

    if (' ' in <!EMPTY_RANGE!>'7' until '7'<!>) { }
}

fun rangeUntil() {
    for (i in <!EMPTY_RANGE!>0..<-1<!>) {}

    for (i in <!EMPTY_RANGE!>2..<2<!>) {}

    if (' ' in <!EMPTY_RANGE!>'7' ..< '7'<!>) { }

    if (0.0F in <!EMPTY_RANGE!>0.0F ..< 0.0F<!>) { }

    if (0.0 in <!EMPTY_RANGE!>0.0 ..< 0.0<!>) { }
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, ifExpression, integerLiteral, localProperty, propertyDeclaration,
rangeExpression */
