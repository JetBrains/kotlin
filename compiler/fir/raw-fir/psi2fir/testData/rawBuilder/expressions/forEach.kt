fun simpleForEach() {
    foreach (i in 1..10) {
        println(i)
    }

    label@ foreach (i in 1..10) {
        if (i == 5) break
        continue@label
    }
}

fun nestedForEachWithAnyLoop() {
    outer@ while (true) {
        foreach (j in 1..10) {
            continue@outer
        }
    }

    outer@ do {
        foreach (j in 1..10) {
            break@outer
        }
    } while (true)

    outer@ for (i in 1..10) {
        for (j in 1..10) {
            foreach (k in 1..10) {
                continue@outer
            }
        }
    }

    outer@ foreach (i in 1..10) {
        for (j in 1..10) {
            continue@outer
        }
    }
}

fun nestedForEachInForEach() {
    outer@ for (i in 1..10) {
        for (j in 1..10) {
            a@ foreach (k in 1..10) {
                for (l in 1..10) {
                    foreach (m in 1..10) {
                        if (l == 4) continue@a
                        break@outer
                    }
                }
            }
        }
    }
}

fun simpleFunctionReturn() {
    foo {
        foreach (i in 1..10) {
            return
        }
    }

    foo {
        foreach (i in 1..10) {
            return@foo
        }
    }

    foreach (i in 1..10) {
        foo {
            return
        }
    }

    foreach (i in 1..10) {
        foo {
            return@foo
        }
    }
}

fun nestedFunctionJump() {
    outer@ for (i in 1..10) {
        foo {
            foreach (j in 10) {
                continue@outer
            }
        }
    }

    // might need to stamp a `foreach` label on the loop just in case
    foo {
        foreach (j in 10) {
            bar {
                break
            }
        }
    }

    var result: String = ""
    result += baz {
        foreach (i in 1..10) {
            if (i == 7) return@baz "test"
            result += i
        }
        "<unreachable>"
    }
}

