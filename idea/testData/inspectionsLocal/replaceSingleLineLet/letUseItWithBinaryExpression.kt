// WITH_RUNTIME
// PROBLEM: none

fun foo() {
    "".let<caret> { it.length + it.length }
}