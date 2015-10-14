inline fun bar(block: () -> String) : String {
    return block()
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

// 10 ALOAD
// 2 ASTORE
// 0 InlineMarker
