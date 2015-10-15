data class MyClass(val x: String?)

fun foo(y: MyClass): Int {
    val z = y.x?.subSequence(0, <!DEBUG_INFO_SMARTCAST!>y.x<!>.length)
    return z?.length ?: -1
}