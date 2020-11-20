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

// JVM_TEMPLATES
// 2 GOTO L7
// 1 GOTO L1

// JVM_IR_TEMPLATES
// 2 GOTO L6
// 1 GOTO L0