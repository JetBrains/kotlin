// WITH_RUNTIME

fun test(s: CharSequence): Int {
    var result = 0
    for (i in s.indices) {
        result = result * 10 + (i + 1)
    }
    return result
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast