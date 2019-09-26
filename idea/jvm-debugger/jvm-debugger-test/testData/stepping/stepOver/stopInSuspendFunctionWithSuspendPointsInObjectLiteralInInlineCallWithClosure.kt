package stopInSuspendFunctionWithSuspendPointsInObjectLiteralInInlineCallWithClosure

import forTests.builder

fun foo(a: Any) {}

suspend fun second() {
    foo("call")
}

interface F {
    suspend fun test() {}
}

inline fun call(f: () -> Unit) {
    f()
}

fun main(args: Array<String>) {
    val inClosure = "first"
    call {
        {
            val some = object : F {
                override suspend fun test() {
                    foo(inClosure)
                    second()
                    //Breakpoint!
                    foo("other")
                }
            }

            builder {
                some.test()
            }
        }()
    }
}
