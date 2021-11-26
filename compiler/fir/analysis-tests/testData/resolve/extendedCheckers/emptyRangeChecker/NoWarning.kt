// WITH_STDLIB

fun foo() {
    for (i in 1..2) { }

    val <!UNUSED_VARIABLE!>a<!> = 3..4

    val v = 1
    if (v in 5..6) { }
}


fun backward() {
    for (i in 2 downTo 1) { }

    val <!UNUSED_VARIABLE!>a<!> = 4 downTo 3

    val v = 1
    if (v in -5 downTo -6) { }
}

fun until() {
    for (i in 1 until 2) { }

    val <!UNUSED_VARIABLE!>a<!> = 3 until 4

    val v = 1
    if (v in -5 until -4) { }
}
