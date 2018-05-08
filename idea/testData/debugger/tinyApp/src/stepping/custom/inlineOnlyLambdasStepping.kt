package inlineOnlyLambdasStepping

class Test
fun test() {}

fun main(args: Array<String>) {
    val data = Test()

    // STEP_OVER: 2
    //Breakpoint! (lambdaOrdinal = 1)
    data.onelinerApply { test() }

    data.multiLineApply { test() }

    // STEP_OVER: 3
    //Breakpoint! (lambdaOrdinal = 1)
    data.onelinerApply2 { test() }

    data.multiLineApply2 { test() }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T> T.onelinerApply(block: T.() -> Unit): T { block(); return this }

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T> T.multiLineApply(block: T.() -> Unit): T {
    block();
    return this
}

inline fun <T> T.onelinerApply2(block: T.() -> Unit): T { block(); return this }

inline fun <T> T.multiLineApply2(block: T.() -> Unit): T {
    block();
    return this
}