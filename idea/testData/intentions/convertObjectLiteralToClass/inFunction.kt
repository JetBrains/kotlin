class Test {
    var field = 1

    fun foo() { // TARGET_BLOCK:
        <caret>object : Runnable {
            override fun run() {
                field = 2
            }
        }
    }
}