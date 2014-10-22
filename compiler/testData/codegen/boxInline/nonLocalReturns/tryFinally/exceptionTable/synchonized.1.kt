import test.*

fun call(): String {
    return nonLocal()
}

inline fun nonLocal(): String {
    mysynchronized(Object()) {
        return "nonLocal"
    }
    return "local"
}

fun box(): String {
    val call = call()
    if (call != "nonLocal") return "fail $call"
    return "OK"
}