// MOVE: up
fun foo() {
    bar {
        /**/ <caret>val foo = 1
        println("foo=")
        println(foo) /**/
    }
}