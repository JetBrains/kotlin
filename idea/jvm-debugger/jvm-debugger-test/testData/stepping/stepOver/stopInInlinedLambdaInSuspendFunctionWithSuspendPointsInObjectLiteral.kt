package stopInInlinedLambdaInSuspendFunctionWithSuspendPointsInObjectLiteral

import forTests.builder

fun foo(a: Any) {}

suspend fun second() { }

interface Bar {
    suspend fun first()
}

inline fun call(f: () -> Unit) {
    f()
}

val bar = object : Bar {
    val t = 121

    override suspend fun first() {
        foo("first")

        call {
            {
                //Breakpoint!
                foo(t)
            }()
        }

        second()

        foo("second")
    }
}

fun main(args: Array<String>) {
    builder {
        bar.first()
    }
}