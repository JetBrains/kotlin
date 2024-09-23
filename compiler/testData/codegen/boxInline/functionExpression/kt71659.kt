// FILE: 1.kt

internal inline fun <T> internalInlineFun(block: () -> T): T = block()
private inline fun <T> privateInlineFun(block: () -> T): T = internalInlineFun(block)

fun testO(): String {
    internalInlineFun<String> { return "O" }
    return "1"
}

fun testK(): String {
    privateInlineFun<String> { return "K" }
    return "2"
}

// FILE: 2.kt

fun box(): String {
    return testO() + testK()
}