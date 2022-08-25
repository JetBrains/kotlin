// FIR_IDENTICAL
// !LANGUAGE: +RangeUntilOperator

fun main(n: Int) {
    for (i in 0<!OPT_IN_USAGE_ERROR!>..<<!>n) {

    }
}
