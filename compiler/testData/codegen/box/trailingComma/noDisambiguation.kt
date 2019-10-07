// !LANGUAGE: +TrailingCommas

fun foo(vararg x: Int) = false
fun foo(x: Int) = true

fun box(): String {
    val x = foo(1)
    val y = foo(1,)
    return if (x && y) "OK" else "ERROR"
}
