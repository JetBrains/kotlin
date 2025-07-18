import kotlin.js.*

@JsExport
fun foo(a : Float): Int = 1

// an error for a duplicate exported name could be useful here
@JsExport
fun foo(a : Double): Int = 2

fun box(): String {
    return "OK"
}