// IS_APPLICABLE: false
fun test(i: Int) {
    <caret>when (i) {
        1 -> {
            println(1)
        }
        2 -> {
            println(2)
        }
        else -> {
            println(3)
        }
    }
}

fun println(i: Int) {}
