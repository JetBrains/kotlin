// NO_CHECK_LAMBDA_INLINING

// FILE: A.kt
class Box<T>(val v: T)
private fun <T> foo(x: T, y: Box<T> = Box<T>(x)) = y.v
internal inline fun useFoo() = foo<String>("O")

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
internal inline fun useBar() = bar<String>("K")

// FILE: B.kt
fun box() : String {
    var result = ""
    result += useFoo()
    result += useBar()
    return result
}
