suspend fun f(x: Any?) {
    x?.let { Unit } ?: Unit
}

fun box(): String {
    return "OK"
}
