operator fun IntRange.forEachWhile(action: (Int) -> Boolean): Unit = forEach {
    if (!action(it)) return
}

fun testSimple() {
    var result = ""

    outer@ foreach (i in 1..10) {
        result += "$i:"
        foreach (j in 1..10) {
            result += j
            foreach (k in 1..10) {
                break@outer
            }
            result += ","
        }
        result += "; "
    }

    if (result != "1:1") throw AssertionError(result)
}

fun testSimpleModified() {
    var result = ""

    outer@ foreach (i in 1..10) {
        result += "$i:"
        foreach (j in 1..10) {
            result += j
            foreach (k in 1..10) {
                continue@outer
            }
            result += ","
        }
        result += "; "
    }

    if (result != "1:12:13:14:15:16:17:18:19:110:1") throw AssertionError(result)
}

fun testMixedJumps() {
    var result = ""

    outer@ for (i in 1..10) {
        result += "$i:"
        for (j in 1..10) {
            a@ foreach (k in 1..10) {
                result += "$j|"
                for (l in 1..10) {
                    result += "$l"
                    foreach (m in 1..10) {
                        if (l == 4) continue@a
                        break@outer
                    }
                    result += "."
                }
            }
            result += ","
        }
        result += "; "
    }

    if (result != "1:1|1") throw AssertionError(result)
}

fun testMixedJumpsModified() {
    var result = ""

    outer@ for (i in 1..10) {
        result += "$i:"
        for (j in 1..5) {
            a@ foreach (k in 1..4) {
                result += "$k|"
                for (l in 1..10) {
                    result += "$l"
                    foreach (m in 1..2) {
                        if (l == 4) continue@a
                        if (k == 3) break@outer
                    }
                    result += "."
                }
            }
            result += ","
        }
        result += "; "
    }

    if (result != "1:1|1.2.3.42|1.2.3.43|1") throw AssertionError(result)
}

inline fun foo(block: () -> Unit): Unit = block()

inline fun bar(block: () -> Unit): Unit = Unit

fun testFunctions() {
    var result = ""
    foo {
        foreach (i in 1..10) {
            result += i
            foo {
                break
            }
        }
    }
    if (result != "1") throw AssertionError(result)
}

inline fun baz(value: () -> String): String = value()

fun testFunctionsWithResult() {
    var result: String = ""
    val output = baz {
        foreach (i in 1..10) {
            if (i == 7) return@baz "test"
            result += i
        }
        "<unreachable>"
    }
    result += output
    if (result != "123456test") throw AssertionError(result)
}

fun box(): String {
    testSimple()
    testSimpleModified()
    testMixedJumps()
    testMixedJumpsModified()
    testFunctions()
    testFunctionsWithResult()
    return "OK"
}
