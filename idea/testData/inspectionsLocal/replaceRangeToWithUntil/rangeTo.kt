// WITH_RUNTIME

fun foo(a: Int) {
    for (i in <caret>0.rangeTo(a - 1)) {

    }
}