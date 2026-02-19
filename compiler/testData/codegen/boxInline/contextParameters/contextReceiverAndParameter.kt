// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// LANGUAGE: +ContextParameters
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt

@OptIn(kotlin.contracts.ExperimentalContracts::class)
public inline fun <T, T1, T2, R> context(with: T, receiver: T1, param: T2, block: context(T) T1.(T2) -> R): R {
    kotlin.contracts.contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return block(with, receiver, param)
}

context(context: A)
public inline fun <A> contextOf(): A = context

// FILE: 2.kt

var result = ""

fun contextOfWithContextParameter() {
    abstract class Logger { abstract fun log(message: String) }
    class StringLogger : Logger() { override fun log(message: String) { result += message } }

    fun <A> withStringLogger(block: context(Logger) String.(String) -> A): A =
        context(StringLogger(), "K", "1") { block(it) }

    withStringLogger {
        contextOf<Logger>().log("O" + this@withStringLogger + it)
    }
}

fun box(): String {
    contextOfWithContextParameter()
    if (result == "OK1") return "OK"
    return result
}
