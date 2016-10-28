// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo() {
    "".let<caret> { it.length + it.length }
}