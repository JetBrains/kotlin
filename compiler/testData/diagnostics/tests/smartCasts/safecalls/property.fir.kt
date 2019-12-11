data class MyClass(val x: String?)

fun foo(y: MyClass): Int {
    val z = y.x?.subSequence(0, y.x.length)
    return z?.length ?: -1
}