// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// LANGUAGE: +ContextParameters
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt

@OptIn(kotlin.contracts.ExperimentalContracts::class)
public inline fun <T, T1, R> context(with: T, receiver: T1, block: context(T) T1.() -> R): R {
    kotlin.contracts.contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return block(with, receiver)
}

context(context: A)
public inline fun <A> contextOf(): A = context

// FILE: 2.kt

var result = ""

fun contextOfWithContextParameter() {
    abstract class Logger { abstract fun log(message: String) }
    class StringLogger : Logger() { override fun log(message: String) { result += message } }

    fun <A> withStringLogger(block: context(Logger) String.() -> A): A =
        context(StringLogger(), "K") { block() }

    withStringLogger {
        contextOf<Logger>().log("O" + this@withStringLogger)
    }
}

fun box(): String {
    contextOfWithContextParameter()
    return result
}
