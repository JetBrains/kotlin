// WITH_STDLIB
// KT-40613
// IGNORE_BACKEND: NATIVE
// KT-65402
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// KT-65403
// IGNORE_BACKEND: WASM

private fun <T> some(): T =
    Some1() as T

private class Some1

private class Some2(private val s: String) {
    fun foo(): String = "Some2: $s"
}

fun box(): String {
    val some2 = try {
        some<Some2>()
    } catch(cce: ClassCastException) {
        val message = cce.toString()
        return if (Regex(".*Some1 cannot be cast to .*Some2.*").matches(message))
            "OK"
        else
            message
    } catch (t: Throwable) {
        return t.toString()
    }
    return some2.foo()
}
