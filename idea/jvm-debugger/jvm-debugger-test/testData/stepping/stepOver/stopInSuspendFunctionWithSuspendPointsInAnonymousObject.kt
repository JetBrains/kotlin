package stopInSuspendFunctionWithSuspendPointsInAnonymousObject

import forTests.builder

fun foo(a: Any) {}

suspend fun second() { }

interface Bar {
    suspend fun first()
}

val bar = object : Bar {
    override suspend fun first() {
        foo("first")

        second()

        //Breakpoint!
        foo("second")
    }
}
                                     
fun main(args: Array<String>) {
    builder {
        bar.first()
    }
}