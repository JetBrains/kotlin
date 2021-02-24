// MOVE: down
fun test(i: Int) {
    <caret>println()
    when (i) {
        1 -> {
            run {
            }
        }
    }
}