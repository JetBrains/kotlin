public object Globals{
    operator fun get(key: String, remove: Boolean = true): String {
        return "OK"
    }
}

fun box(): String {
    return Globals["test"]
}
