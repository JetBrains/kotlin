// !OPT_IN: kotlin.js.ExperimentalJsExport
// FIR_IDENTICAL
@file:JsExport

interface Optional<out T> {
    @JsName("valueOrThrowException")
    fun valueOrThrow(exp: Throwable): T

    fun valueOrThrow(): T = valueOrThrow(NoSuchElementException("Optional has no value"))
}

abstract class None<out T : Any> private constructor() : Optional<T> {
    companion object : None<Nothing>()
    override fun valueOrThrow(exp: Throwable): Nothing = throw exp
}
