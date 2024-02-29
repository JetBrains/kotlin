fun <T> foo(p: T, mapper : (T) -> String): String {
    mapper(p)
}

fun bar() {
    foo(1, x<caret>y)
}
