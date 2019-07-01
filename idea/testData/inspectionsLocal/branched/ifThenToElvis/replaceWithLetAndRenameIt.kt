// WITH_RUNTIME

fun foo(it: String?) {
    val x = <caret>if (it != null) {
        bar(it)
    }
    else {
        13
    }
}

fun bar(s: String): Int = 42