
inline fun bar(x: Int) : Int {
    return x
}

fun foobar(x: Int, y: Int, z: Int) = x + y + z

fun foo() : Int {
    return foobar(1, bar(2), 3)
}

// 1 ISTORE
// 5 ILOAD
// 0 InlineMarker
