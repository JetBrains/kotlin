// MOVE: up
fun foo() {
    run(1, 2) {
        println("bar")
    }
    <caret>println("foo")
}