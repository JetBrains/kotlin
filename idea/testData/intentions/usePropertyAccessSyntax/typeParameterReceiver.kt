// WITH_RUNTIME

fun <T : Thread> foo(t: T) {
    t.<caret>setDaemon(true)
}

