// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74389
// WITH_STDLIB

@file:OptIn(ExperimentalJsExport::class)

import kotlin.js.Promise

@JsExport
fun foo(): Promise<*> = null!!

@JsExport
fun bar(): Promise<Unit> = null!!

@JsExport
class Box<T>

<!NON_EXPORTABLE_TYPE!>@JsExport
fun box(): Box<*><!> = null!!

@JsExport
fun <T> promisify(x: T): Box<out T> =
    null!!
