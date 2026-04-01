// WITH_STDLIB

// FILE: lib.kt
val log = StringBuilder()

inline fun test() {
    val localLazy by lazy {
        log.append("localLazy;")
        "v;"
    }
    log.append("test;")
    log.append(localLazy)
}

// FILE: main.kt
fun box(): String {
    test()
    val t = log.toString()
    if (t != "test;localLazy;v;")
        throw AssertionError(t)
    return "OK"
}