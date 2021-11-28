@CompileTimeCalculation
fun foo(i: Int): Int = foo(i + 1)
const val overflow = <!WAS_NOT_EVALUATED: `
Exception java.lang.StackOverflowError
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	at StackOverflowKt.foo(stackOverflow.kt:2)
	...`!>foo(0)<!>

@CompileTimeCalculation
fun withPossibleOverflow(x: Int): Int {
    if (x == 0) return 0
    return withPossibleOverflow(x - 1) + 1
}
const val notOverflow = <!EVALUATED: `5000`!>withPossibleOverflow(5_000)<!>
