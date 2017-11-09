// WITH_RUNTIME
fun test() {
    val x = <caret>when {
        true -> {
            println(1)
            1
        }
        else -> {
            println(2)
            2
        }
    }
}