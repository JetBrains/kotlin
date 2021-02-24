// WITH_RUNTIME
fun foo(<caret>p: Any) {
    object : Runnable {
        override fun run() {
            print(this)
        }
    }
}