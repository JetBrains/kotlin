fun <T> myRun(block: () -> T): T = block()

fun foo() {}

fun test() {
    myRun {
        try {
            val x = 1
        } catch(e: Exception) {
            foo()
        }
    }
}