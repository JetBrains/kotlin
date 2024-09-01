// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND_K1: ANY
// Reason: break/continue in inline lambdas unsupported

inline fun myRunInline(block: () -> Unit): Unit = block()

fun testContinue1(): String {
    while (true) {
        myRunInline {
            try {
                continue
            } finally {
                return "OK"
            }
        }
    }
    return "Fail"
}

fun testBreak1(): String {
    while (true) {
        myRunInline {
            try {
                break
            } finally {
                return "OK"
            }
        }
    }
    return "Fail"
}

fun testContinue2(): Int {
    var x = 0
    for (i in 0..1) {
        myRunInline {
            try {
                x += 1
                continue
                x += 10
            } finally {
                x += 100
            }
        }
    }
    return x
}

fun testBreak2(): Int {
    var x = 0
    while (true) {
        myRunInline {
            try {
                x += 1
                break
                x += 10
            } finally {
                x += 100
            }
        }
    }
    return x
}

fun testNested(): Int {
    var x = 0
    for (i in 0..1) {
        myRunInline {
            try {
                x += 1
                for (j in 0..1) {
                    myRunInline {
                        try {
                            x += 10
                            continue
                            x += 100
                        } finally {
                            x += 1000
                        }
                    }
                }
                continue
            } finally {
                x += 10000
            }
        }
    }
    return x
}

fun box(): String {
    if (testContinue1() != "OK") return "Fail 1"
    if (testBreak1() != "OK") return "Fail 2"
    if (testContinue2() != 202) return testContinue2().toString()
    if (testBreak2() != 101) return testBreak2().toString()
    if (testNested() != 24042) return testNested().toString()

    return "OK"
}