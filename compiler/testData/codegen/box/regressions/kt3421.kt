public object Globals{
    public fun get(key: String, remove: Boolean = true): String {
        return "OK"
    }
}

fun box(): String {
    return Globals["test"]
}
