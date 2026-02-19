private fun interface MyRunnable {
    fun bar()
}

fun test() {
    My<caret>Runnable {}
}