fun <T : Any, R> T.let(f: (T) -> R): R = f(this)

fun box(): String {
    val o: String? = null

    var state = 0

    o?.let {
        state = 1
    } ?: ({ state = 2 })()

    if (state != 2) return "Fail: $state"
    return "OK"
}
