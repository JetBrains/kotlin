inline fun <T, reified K> bar() {}

fun foo() {
    <!INAPPLICABLE_CANDIDATE!>bar<!><Int>()
}
