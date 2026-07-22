// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

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
