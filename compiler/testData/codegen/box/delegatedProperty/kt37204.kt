// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

val log = StringBuilder()

inline fun test() {
    val localLazy by lazy {
        log.append("localLazy;")
        "v;"
    }
    log.append("test;")
    log.append(localLazy)
}

fun box(): String {
    test()
    val t = log.toString()
    if (t != "test;localLazy;v;")
        throw AssertionError(t)
    return "OK"
}