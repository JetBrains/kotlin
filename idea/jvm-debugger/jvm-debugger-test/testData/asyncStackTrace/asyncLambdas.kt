package asyncLambdas

suspend fun main() {
    foo()
}

val foo: suspend () -> Unit = {
    val a = 5
    bar()
}

val bar: suspend () -> Unit = {
    val b = 3
    baz()
}

val baz: suspend () -> Unit = {
    //Breakpoint!
    val a = 5
}