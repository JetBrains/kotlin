// WITH_RUNTIME

fun foo(a: Long) {
    for (i in 1L<caret>..a - 1L) {

    }
}