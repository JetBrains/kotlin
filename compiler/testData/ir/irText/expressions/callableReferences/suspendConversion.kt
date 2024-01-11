// FIR_IDENTICAL
// !LANGUAGE: +SuspendConversion

fun useSuspend(fn: suspend () -> Unit) {}
fun useSuspendNullable(fn: (suspend () -> Unit)?) {}
fun useSuspendNestedNullable(fn: ((suspend () -> Unit)?)?) {}
fun useSuspendInt(fn: suspend (Int) -> Unit) {}

suspend fun foo0() {}
fun foo1() {}
fun fooInt(x: Int) {}
fun foo2(vararg xs: Int) {}
fun foo3(): Int = 42
fun foo4(i: Int = 42) {}

class C {
    fun bar() {}
}

fun testLambda() { useSuspend { foo1() } }

fun testNoCoversion() { useSuspend(::foo0) }

fun testSuspendPlain() { useSuspend(::foo1) }

fun testSuspendWithArgs() { useSuspendInt(::fooInt) }

fun testWithVararg() { useSuspend(::foo2) }

fun testWithVarargMapped() { useSuspendInt(::foo2) }

fun testWithCoercionToUnit() { useSuspend(::foo3) }

fun testWithDefaults() { useSuspend(::foo4) }

fun testWithBoundReceiver() { useSuspend(C()::bar) }

fun testNullableParam() { useSuspendNullable(::foo1) }

fun testNestedNullableParam() { useSuspendNestedNullable(::foo1) }