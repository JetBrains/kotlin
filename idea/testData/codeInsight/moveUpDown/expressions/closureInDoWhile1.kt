// MOVE: down
fun foo(i: Int) {
    do {
        println(i)
        <caret>run {
        }
    } while (i in run { 1..2 })
}