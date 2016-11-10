// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo() {
    "".let<caret> { it.substring(0, 1) + it }
}