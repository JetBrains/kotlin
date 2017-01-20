@Suppress("NOTHING_TO_INLINE")
inline fun foo(s4: String, s5: String): String {
    return s4 + s5
}

fun bar(s1: String, s2: String, s3: String): String {
    return s1 + foo(s2, s3)
}

fun main(args: Array<String>) {
    println(bar("Hello ", "wor", "ld"))
}


