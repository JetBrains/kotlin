fun String.<caret>f(p: Int): Int {
    println(p)
    return hashCode() * p
}

fun f(s: String?) {
    s?.f(1)
    s?.substring(1)?.f(2)
    val s1 = s?.f(3)
    val s2 = s?.substring(1)?.f(4)
}