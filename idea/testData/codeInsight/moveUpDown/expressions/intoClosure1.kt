// MOVE: down
fun foo() {
    <caret>println("foo")
    run(1, 2) {
        println("bar")
    }
}