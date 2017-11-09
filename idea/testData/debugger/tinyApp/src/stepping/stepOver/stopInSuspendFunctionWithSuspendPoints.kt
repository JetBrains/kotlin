package stopInSuspendFunctionWithSuspendPoints

import forTests.builder

fun foo(a: Any) {}

suspend fun second() {
}

suspend fun first() {
    foo("first")

    second()

    //Breakpoint!
    foo("second")
}
                                     
fun main(args: Array<String>) {
    builder {
        first()
    }
}