fun test(x: String?) {
    if (x !is String) return

    if (x == null) println("dead code")
}

// 0 println