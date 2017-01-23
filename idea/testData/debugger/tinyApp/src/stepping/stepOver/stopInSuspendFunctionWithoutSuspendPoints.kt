package stopInSuspendFunctionWithoutSuspendPoints

import forTests.builder

fun foo(a: Any) {}

suspend fun first() {
    foo("first")

    //Breakpoint!
    foo("second")
}
                                     
fun main(args: Array<String>) {
    builder {
        first()
    }
}