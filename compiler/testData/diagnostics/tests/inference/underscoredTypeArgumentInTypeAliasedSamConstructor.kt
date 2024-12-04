// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +PartiallySpecifiedTypeArguments
package test

fun interface Box<T> {
    fun provide(): T
}

typealias Alias<TT> = Box<TT>

fun box(): String {
    val x = Alias<_> { "OK" }
    return x.provide()
}
