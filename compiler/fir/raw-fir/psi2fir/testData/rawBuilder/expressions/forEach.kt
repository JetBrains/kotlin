fun simpleForEach() {
    miau (i in 1..10) {
        println(i)
    }

    label@ miau (i in 1..10) {
        if (i == 5) break
        continue@label
    }
}

fun nestedForEachWithAnyLoop() {
    outer@ while (true) {
        miau (j in 1..10) {
            continue@outer
        }
    }

    outer@ do {
        miau (j in 1..10) {
            break@outer
        }
    } while (true)

    outer@ for (i in 1..10) {
        for (j in 1..10) {
            miau (k in 1..10) {
                continue@outer
            }
        }
    }

    outer@ miau (i in 1..10) {
        for (j in 1..10) {
            continue@outer
        }
    }
}

fun nestedForEachInForEach() {
    outer@ for (i in 1..10) {
        for (j in 1..10) {
            a@ miau (k in 1..10) {
                for (l in 1..10) {
                    miau (m in 1..10) {
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
        miau (i in 1..10) {
            return
        }
    }

    foo {
        miau (i in 1..10) {
            return@foo
        }
    }

    miau (i in 1..10) {
        foo {
            return
        }
    }

    miau (i in 1..10) {
        foo {
            return@foo
        }
    }
}

fun nestedFunctionJump() {
    outer@ for (i in 1..10) {
        foo {
            miau (j in 10) {
                continue@outer
            }
        }
    }

    // might need to stamp a `miau` label on the loop just in case
    foo {
        miau (j in 10) {
            bar {
                break
            }
        }
    }

    var result: String = ""
    result += baz {
        miau (i in 1..10) {
            if (i == 7) return@baz "test"
            result += i
        }
        "<unreachable>"
    }
}

