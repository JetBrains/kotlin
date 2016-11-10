// WITH_RUNTIME
// IS_APPLICABLE: true

fun foo() {
    "".let<caret> { it + 1 }
}