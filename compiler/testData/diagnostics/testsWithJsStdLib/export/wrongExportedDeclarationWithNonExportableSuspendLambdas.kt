// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +JsAllowExportingSuspendFunctions -JsExportingSuspendLambdas
package foo

<!NON_EXPORTABLE_TYPE!>@JsExport
val exportedSuspendLambda: suspend () -> String<!> = { "OK" }

<!NON_EXPORTABLE_TYPE!>@JsExport
fun produceSuspendLambda(): suspend (Int) -> Int<!> = { x -> x * 2 }

@JsExport
suspend fun runLambda(<!NON_EXPORTABLE_TYPE!>callback: suspend (Int) -> Int<!>): Int = callback(21) * 2

@JsExport
suspend fun runVoidLambda(<!NON_EXPORTABLE_TYPE!>callback: suspend () -> Unit<!>) {
    callback()
}

<!NON_EXPORTABLE_TYPE!>@JsExport
fun roundTrip(<!NON_EXPORTABLE_TYPE!>callback: suspend (Int) -> Int<!>): suspend (Int) -> Int<!> =
    { x -> callback(x) + 1 }

@JsExport
class LambdaHolder(private val base: Int) {
    <!NON_EXPORTABLE_TYPE!>val multiplier: suspend (Int, Int) -> Int<!> = { x, y -> x * y }

    <!NON_EXPORTABLE_TYPE!>fun produceAdder(): suspend (Int) -> Int<!> = { x -> x + base }

    suspend fun apply(<!NON_EXPORTABLE_TYPE!>cb: suspend (Int) -> Int<!>, x: Int): Int = cb(x)
}

<!NON_EXPORTABLE_TYPE!>@JsExport
fun produceArrayOfSuspendLambdas(): Array<suspend (Int) -> Int><!> = arrayOf(
    { x -> x + 1 },
    { x -> x * 2 },
)

@JsExport
suspend fun reduceArrayOfSuspendLambdas(<!NON_EXPORTABLE_TYPE!>lambdas: Array<suspend (Int) -> Int><!>, start: Int): Int {
    var acc = start
    for (lambda in lambdas) {
        acc = lambda(acc)
    }
    return acc
}

@JsExport
interface InterfaceWithSuspendLambdaProp {
    <!NON_EXPORTABLE_TYPE!>val handler: suspend (Int) -> String<!>
}

@JsExport
abstract class AbstractClassWithSuspendLambdaProp {
    <!NON_EXPORTABLE_TYPE!>abstract val handler: suspend (Int) -> String<!>
}
