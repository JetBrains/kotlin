// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun testArray() {
    Array<String>(5) { i ->
        if (i == 3) return
        i.toString()
    }
    throw AssertionError()
}

fun testIntArray() {
    IntArray(5) { i ->
        if (i == 3) return
        i
    }
    throw AssertionError()
}

fun box(): String {
    testArray()
    testIntArray()
    return "OK"
}
