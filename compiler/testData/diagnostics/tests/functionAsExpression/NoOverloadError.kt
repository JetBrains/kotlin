// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

val bar = fun name() {}
val bas = fun name() {}

fun gar(p: Any?) = fun name() {}
fun gas(p: Any?) = fun name() {}

fun outer() {
    val bar = fun name() {}
    val bas = fun name() {}

    fun gar(p: Any?) = fun name() {}
    fun gas(p: Any?) = fun name() {}

    gar(fun name() {})
    gar(fun name() {})
}