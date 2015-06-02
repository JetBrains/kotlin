package test

fun a(b: () -> String) : String {
    return b()
}

inline fun test(l: () -> String): String {
    return l()
}