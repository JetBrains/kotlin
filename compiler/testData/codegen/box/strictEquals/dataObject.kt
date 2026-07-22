// LANGUAGE: +StrictEquals

data object Singleton {
    val tag: String = "singleton"
}

fun box(): String {
    if (Singleton != Singleton) return "FAIL 1"
    val any: Any? = Singleton
    if (Singleton != any) return "FAIL 2"
    if (Singleton == any) {
        if (any.tag != "singleton") return "FAIL 3"
    }
    val other: Any? = "not singleton"
    if (Singleton == other) return "FAIL 4"
    return "OK"
}
