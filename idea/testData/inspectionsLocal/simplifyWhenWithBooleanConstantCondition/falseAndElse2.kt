// WITH_RUNTIME
fun test() {
    val x = <caret>when {
        false -> {
            println(1)
            1
        }
        else -> {
            println(2)
            2
        }
    }
}