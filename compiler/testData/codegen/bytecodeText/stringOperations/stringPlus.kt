fun f(s: String?, t: String): String {
    return s.plus(t)
}

fun g(s: String, t: Any?): String {
    return "$s$t"
}

fun h(s: String, t: Any?): String {
    return s + t
}

// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.stringPlus
