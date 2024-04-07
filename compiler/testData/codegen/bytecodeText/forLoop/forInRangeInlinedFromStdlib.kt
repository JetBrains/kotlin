// WITH_STDLIB

fun f(array: Array<Int>): Int {
    return array.maxBy { -it }
}

// 0 IntRange
// 0 iterator