// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

class MyCharSequence(val s: String) : CharSequence {
    fun get(foo: String): Char = TODO("shouldn't be called!")
    override val length = s.length
    override fun subSequence(startIndex: Int, endIndex: Int) = s.subSequence(startIndex, endIndex)
    override fun get(index: Int) = s.get(index)
}

fun box(): String {
    val cs = MyCharSequence("1234")
    val result = StringBuilder()
    for (c in cs) {
        result.append(c)
    }
    assertEquals("1234", result.toString())

    return "OK"
}