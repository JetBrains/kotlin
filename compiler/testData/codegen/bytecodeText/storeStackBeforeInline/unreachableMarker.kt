// IGNORE_BACKEND: JVM_IR
inline fun <T> runAfterLoop(fn: () -> T): T {
    for (i in 1..2);
    return fn()
}

inline fun bar(block: () -> String) : String {
    return runAfterLoop(block)
}

inline fun bar2() : String {
    return bar { return "def" }
}

fun foobar(x: String, y: String, z: String) = x + y + z

fun foo() : String {
    return foobar(
            "abc",
            bar2(),
            "ghi"
    )
}

// 12 ALOAD
// 2 ASTORE
// 0 InlineMarker
