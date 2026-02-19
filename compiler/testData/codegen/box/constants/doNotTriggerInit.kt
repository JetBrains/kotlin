var initialized = 0

object O {
    init {
        initialized += 1
    }
    const val x = 1
    val y = 2
}

fun box() : String {
    if (O.x != 1) return "FAIL 1"
    if (initialized != 0) return "FAIL 2"
    if (O.y != 2) return "FAIL 3"
    if (initialized != 1) return "FAIL 4"
    return "OK"
}