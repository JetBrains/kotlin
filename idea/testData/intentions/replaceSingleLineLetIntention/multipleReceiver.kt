// WITH_RUNTIME
// IS_APPLICABLE: false

fun baz(foo: String) {
    foo.let<caret> { it.substringAfterLast(it.capitalize()) }
}
