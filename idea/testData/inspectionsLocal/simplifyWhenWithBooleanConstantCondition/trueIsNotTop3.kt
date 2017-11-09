// WITH_RUNTIME
fun test(i: Int) {
    <caret>when {
        i == 1 -> {
            println(1)
        }
        false -> {
            println(2)
        }
        true -> {
            println(3)
        }
        else -> {
            println(4)
        }
    }
}