// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +PartiallySpecifiedTypeArguments
package test

class Box<T>(val value: T)

typealias Alias<TT> = Box<TT>

fun box(): String {
    val x = Alias<_>("OK")
    return x.value
}
