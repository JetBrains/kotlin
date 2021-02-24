// WITH_RUNTIME

fun bar(p: Int) {
    Thread(<caret>object: Runnable {
        override fun run() {
            if (p < 0) return
            throw UnsupportedOperationException()
        }
    })
}