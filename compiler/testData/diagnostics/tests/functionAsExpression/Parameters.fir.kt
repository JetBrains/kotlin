// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE

val bar = fun(p: Int = 3) {}
val bas = fun(vararg p: Int) {}

fun gar() = fun(p: Int = 3) {}
fun gas() = fun(vararg p: Int) {}

fun outer(b: Any?) {
    val bar = fun(p: Int = 3) {}
    val bas = fun(vararg p: Int) {}

    fun gar() = fun(p: Int = 3) {}
    fun gas() = fun(vararg p: Int) {}

    outer(fun(p: Int = 3) {})
    outer(fun(vararg p: Int) {})
}