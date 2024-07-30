// FIR_IDENTICAL
// ISSUE: KT-69965
// IGNORE_BACKEND: JS_IR
// REASON: KT-69965 missing @[Source] annotation on return type of IrCall to `fun source()`

// MODULE: lib
// FILE: lib.kt
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Target(TYPE, VALUE_PARAMETER)
@Retention(SOURCE)
annotation class Source

@Target(TYPE, VALUE_PARAMETER)
@Retention(BINARY)
annotation class Binary

@Target(TYPE, VALUE_PARAMETER)
@Retention(RUNTIME)
annotation class Runtime

fun source(@Source x: @Source Short): @Source Short = x
fun binary(@Binary x: @Binary Short): @Binary Short = x
fun runtime(@Runtime x: @Runtime Short): @Runtime Short = x

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    source(0)
    binary(1)
    runtime(2)

    return "OK"
}
