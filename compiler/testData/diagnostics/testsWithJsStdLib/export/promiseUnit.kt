// FIR_IDENTICAL
// ISSUE: KT-57192
// Promise<Unit> wrongly raised NON_EXPORTABLE_TYPE
// IGNORE_BACKEND: JS_IR

@file:OptIn(ExperimentalJsExport::class)
import kotlin.js.Promise

@JsExport
fun fooInt(p: Promise<Int>): Promise<Int>? = p

@JsExport
fun fooUnitReturn(): Promise<Unit>? = null

@JsExport
fun fooUnitArgument(p: Promise<Unit>) {
    p.then {}
}

@JsExport
interface I<T> {
    fun bar(): T
}

@JsExport
fun fooIIntArgument(i: I<Int>) = i.bar()

@JsExport
fun fooIUnitArgument(i: I<Unit>) = i.bar()
