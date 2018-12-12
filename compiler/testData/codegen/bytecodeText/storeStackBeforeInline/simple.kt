// IGNORE_BACKEND: JVM_IR

inline fun bar(x: Int) : Int {
    return x
}

fun foobar(x: Int, y: Int, z: Int) = x + y + z

fun foo() : Int {
    return foobar(1, bar(2), 3)
}

// fake inline variables occupy 2 ISTOREs.
// 5 ISTORE
// 7 ILOAD
// 0 InlineMarker
