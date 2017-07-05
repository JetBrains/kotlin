// WITH_RUNTIME

fun foo() {
    val a = 1L
    val b = 2L
    a..b step<caret> 1L
}