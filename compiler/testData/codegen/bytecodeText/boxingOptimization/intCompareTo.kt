fun box(): String {
    val a: Any = 1
    val b: Any = 42
    val test = (a as Comparable<Any>).compareTo(b)
    if (test != -1) return "Fail: $test"

    return "OK"
}

// 0 compareTo
// 1 Intrinsics.compare