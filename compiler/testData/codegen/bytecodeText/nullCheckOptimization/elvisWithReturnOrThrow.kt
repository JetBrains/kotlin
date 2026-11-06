fun f(s: String?): Int = (s ?: return -1).length

fun g(s: String?, e: Throwable): Int = (s ?: throw e).length

fun h(s: String?): Int {
    val b = s ?: return 42
    return b.length
}

fun plain(s: String?): Int = (s ?: "abc").length

// 1 DUP
//  ^ only the plain elvis keeps the value on the stack across the null check
