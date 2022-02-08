// WITH_STDLIB

fun box(): String {
    var r = test()
    if (r != "01") throw AssertionError(r.toString())
    return "OK"
}

private fun test(): String {
    var r = ""
    for ((i, _) in (1..'c' - 'a').withIndex()) {
        r += i.toString()
    }
    return r
}