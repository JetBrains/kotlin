// WITH_RUNTIME

fun foo(s: String?) {
    val x = <caret>if (s != null) {
        bar(s)
    }
    else {
        13
    }
}

fun bar(s: String): Int = 42