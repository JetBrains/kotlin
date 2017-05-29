package stepIntoSuspendFunctionSimple

import forTests.builder

private fun foo() {}

suspend fun second() {
}

suspend fun first(): Int {
    second()
    return 12
}

fun main(args: Array<String>) {
    builder {
        //Breakpoint!
        first()
        foo()
    }
}