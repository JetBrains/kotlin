// IGNORE_BACKEND: JVM_IR
fun test(x: Int, y: Int): String {
    var result: String
    if (x == 6) {
        if (y == 6) {
            result = "a"
        } else {
            result = "b"
        }
    } else {
        result = "c"
    }
    return result
}

fun infiniteLoop() {
    while(true) {}
}

// 2 GOTO L7
// 1 GOTO L1