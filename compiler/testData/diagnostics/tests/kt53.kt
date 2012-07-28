val <T, E> T.foo : E?
    get() = null

fun test(): Int? {
    return 1.foo
}