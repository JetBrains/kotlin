// WITH_RUNTIME
// PROBLEM: none

fun foo() {
    val a = 1L
    val b = 2L
    a..b step<caret> 2L
}