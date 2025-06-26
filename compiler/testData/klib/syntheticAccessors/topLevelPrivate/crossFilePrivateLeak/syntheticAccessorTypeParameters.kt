// NO_CHECK_LAMBDA_INLINING

// FILE: A.kt
class Box<T>(val v: T)
private fun <T> foo(x: T, y: Box<T> = Box<T>(x)) = y.v
internal inline fun useFoo() = foo<String>("OK1")

private fun <T> bar(
    x: T,
    y: Box<T> = run {
        class LocalBox(val v: T) {
            fun toBox() = Box(v)
        }
        val tmp = LocalBox(x)
        tmp.toBox()
    }
) = y.v
internal inline fun useBar() = bar<String>("OK2")

private fun <T> baz(x: T, vararg y: T): T = if (y.size > 0) y[0] else x
internal inline fun useBaz() = baz("", "OK3", "")

// FILE: B.kt
fun box() : String {
    var result = ""
    result += useFoo()
    result += " "
    result += useBar()
    result += " "
    result += useBaz()
    if (result != "OK1 OK2 OK3") return result
    return "OK"
}
