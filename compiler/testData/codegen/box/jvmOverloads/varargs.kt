// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

class C {
    @JvmOverloads
    fun foo(bar: Int = 0, vararg status: String) {

    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo", Array<String>::class.java)
    return if (m.isVarArgs) "OK" else "fail"
}
