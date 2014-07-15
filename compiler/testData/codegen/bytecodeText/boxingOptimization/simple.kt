
inline fun foo(x : Int, block : (Int) -> Int) : Int {
    return block(x)
}

fun bar() {
    foo(1) { x -> x + 1 }
}

// 1 java/lang/Integer.valueOf
// 1 intValue
