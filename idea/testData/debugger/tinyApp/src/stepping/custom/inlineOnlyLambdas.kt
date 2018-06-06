package inlineOnlyLambdas

class Test
fun test() {}

fun main(args: Array<String>) {
    val data = Test()

    //Breakpoint! (lambdaOrdinal = 1)
    data.onelinerApply { test() }

    //Breakpoint! (lambdaOrdinal = 1)
    data.multiLineApply { test() }

    //Breakpoint! (lambdaOrdinal = 1)
    data.onelinerApply2 { test() }

    //Breakpoint! (lambdaOrdinal = 1)
    data.multiLineApply2 { test() }

    //Breakpoint!
    data.withoutLambdaParams()
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T> T.withoutLambdaParams(): T {
    return this
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

// RESUME: 5