import kotlin.js.*

@JsExport
fun foo(a : Float): Int = 1

@JsExport
fun foo(a : Double): Int = 2

fun box(): String {
    return "OK"
}