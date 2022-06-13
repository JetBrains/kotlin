fun <T : Any, R> T.myLet(f: (T) -> R): R = f(this)

fun box(): String {
    val o: String? = null

    var state = 0

    o?.myLet {
        state = 1
    } ?: { state = 2 }.let { it() }

    if (state != 2) return "Fail: $state"
    return "OK"
}
