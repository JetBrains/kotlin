// WITH_STDLIB

fun foo() {
    for (i in <!EMPTY_RANGE!>2..1<!>) { }

    val <!UNUSED_VARIABLE!>a<!> = <!EMPTY_RANGE!>10..0<!>

    val v = 1
    if (v in <!EMPTY_RANGE!>10..1<!>) { }
}

fun backward() {
    for (i in <!EMPTY_RANGE!>1 downTo 2<!>) { }

    val <!UNUSED_VARIABLE!>a<!> = <!EMPTY_RANGE!>-3 downTo 4<!>

    val v = 1
    if (v in <!EMPTY_RANGE!>0 downTo 6<!>) { }
}

fun until() {
    for (i in <!EMPTY_RANGE!>1 until 1<!>) { }

    val <!UNUSED_VARIABLE!>a<!> = <!EMPTY_RANGE!>4 until 3<!>

    val v = 1
    if (v in <!EMPTY_RANGE!>-5 until -5<!>) { }
}
