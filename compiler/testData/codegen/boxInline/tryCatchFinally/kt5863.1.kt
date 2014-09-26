inline fun test2Inline() = performWithFinally { "OK" }

fun box(): String {
    return test2Inline()
}

