// IGNORE_BACKEND: JVM_IR
fun test(x: String?) {
    if (x == null) return

    if (x == null) println("dead code")
}

// 0 println