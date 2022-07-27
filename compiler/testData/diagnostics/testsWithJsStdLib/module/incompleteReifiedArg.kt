// FIR_IGNORE
// FIR_IDENTICAL
inline fun <T, reified K> bar() {}

fun foo() {
    bar<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>()
}