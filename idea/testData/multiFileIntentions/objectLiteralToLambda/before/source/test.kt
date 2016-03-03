package source

fun bar() {
    J.foo(<caret>object : Runnable {
        override fun run() {
            println("a")
        }
    }, 1)
}