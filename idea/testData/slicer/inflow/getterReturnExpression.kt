// FLOW: IN

val foo: Int
    get(): Int {
        return 0
    }

fun test() {
    val <caret>x = foo
}