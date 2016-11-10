// WITH_RUNTIME
// IS_APPLICABLE: false

fun baz(foo: String) {
    foo.let<caret> { it.indexOfLast { c -> c == it[0] } + 1 }
}