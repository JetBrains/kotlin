// MOVE: up
fun foo() {
    run(1, 2) {
        <caret>println("foo")
        println("bar")
    }
}