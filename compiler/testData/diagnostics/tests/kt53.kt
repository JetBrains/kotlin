val <T> T.foo : T<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>
    get() = null

fun test(): Int? {
    return 1.foo
}