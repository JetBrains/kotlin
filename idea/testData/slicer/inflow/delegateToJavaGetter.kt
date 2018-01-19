// FLOW: IN
// RUNTIME_WITH_REFLECT

val foo: Int by D.INSTANCE

fun test() {
    val <caret>x = foo
}