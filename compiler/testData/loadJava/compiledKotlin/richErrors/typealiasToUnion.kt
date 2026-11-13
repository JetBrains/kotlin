// LANGUAGE: +RichErrors
// WITH_STDLIB
package test

error class Foo
error class Bar

typealias Errs = Foo | Bar
typealias Res<T> = List<T> | Foo
typealias S = String
typealias SErrs = S | Errs

fun f1(): Errs = Foo()
fun f2(): Res<String> = listOf("")
fun f3(): Res<S> = listOf("")
fun f4(): Errs? = null
fun f5(x: Res<Int>?): String | Errs = ""
fun <T> f6(x: Res<T>): Res<T> = x
fun f7(): S | Foo = ""
fun f8(): SErrs = ""
