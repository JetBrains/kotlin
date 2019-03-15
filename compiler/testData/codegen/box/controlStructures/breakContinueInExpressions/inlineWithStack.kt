// IGNORE_BACKEND: JVM_IR
inline fun bar(block: () -> String) : String {
    return block()
}

inline fun bar2() : String {
    while (true) break
    return bar { return "def" }
}

fun foobar(x: String, y: String, z: String) = x + y + z

fun box(): String {
    val test = foobar("abc", bar2(), "ghi")
    return if (test == "abcdefghi")
        "OK"
    else "Failed, test=$test"
}