// FIR_IDENTICAL
// ISSUE: KT-58284

fun foo() {
    for (i in <!ITERATOR_MISSING!>0<!>) {}
}

fun String.iterator(): Iterator<Int> = TODO()
