// WITH_RUNTIME
fun test(i: Int) {
    val x = <caret>when {
        i == 1 -> {
            println(1)
            1
        }
        false -> {
            println(2)
            2
        }
        true -> {
            println(3)
            3
        }
        else -> {
            println(4)
            4
        }
    }
}