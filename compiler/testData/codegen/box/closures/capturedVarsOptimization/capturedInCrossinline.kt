inline fun runCrossInline(crossinline f: () -> Unit) {
    f()
}

fun box(): String {
    var x = ""
    runCrossInline { x = "OK" }
    return x
}