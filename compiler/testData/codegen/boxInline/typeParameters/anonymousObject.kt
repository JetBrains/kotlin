// FILE: 1.kt
interface I<TTTT> {
    fun get(): TTTT
    fun set(x: TTTT)

}
interface J
class X(val result: String) : J
class Box<T>(val x : T) {
    inline fun getI(crossinline block : () -> Unit) : I<T> {
        val temp = x
        return object : I<T> {
            var t = temp
            override fun get() = t
            override fun set(y: T) {
                block()
                t = y
            }
        }
    }
}

fun Box<*>.getIExt() = getI() {}

// FILE: 2.kt
fun box(): String {
    val xObject = Box(X("OK")).getIExt().get() as X
    return xObject.result
}