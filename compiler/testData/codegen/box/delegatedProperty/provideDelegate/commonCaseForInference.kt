// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

object CommonCase {
    interface Fas<D, E, R>

    fun <D, E, R> delegate() : Fas<D, E, R> = object : Fas<D, E, R> {}

    operator fun <D, E, R> Fas<D, E, R>.provideDelegate(host: D, p: Any?): Fas<D, E, R> = this
    operator fun <D, E, R> Fas<D, E, R>.getValue(receiver: E, p: Any?): R = "OK" as R

    val Long.test1: String by delegate()
    val Long.test2: String by delegate<CommonCase, Long, String>()
}

fun box() = with(CommonCase) {
    require(3L.test1 == "OK" && 3L.test2 == "OK")
    "OK"
}
