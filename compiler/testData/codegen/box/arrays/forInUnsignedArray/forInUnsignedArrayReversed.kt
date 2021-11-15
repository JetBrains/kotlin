// WITH_STDLIB

fun test(uis: UIntArray): String {
    var s = ""
    for (ui in uis.reversed()) {
        s += ui
    }
    return s
}


fun box(): String {
    val test = test(uintArrayOf(1U, 2U, 3U))
    if (test != "321") return "Failed: $test"
    return "OK"
}
