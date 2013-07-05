// MOVE: up
fun foo(i: Int) {
    while (i in run { 1..2 }) {
        <caret>run {
        }
        println(i)
    }
}