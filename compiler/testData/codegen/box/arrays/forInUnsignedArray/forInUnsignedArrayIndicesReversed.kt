// WITH_STDLIB

fun test(uis: UIntArray): String {
    var s = ""
    for (i in uis.indices.reversed()) {
        s += "$i:${uis[i]};"
    }
    return s
}


fun box(): String {
    val test = test(uintArrayOf(1U, 2U, 3U))
    if (test != "2:3;1:2;0:1;") return "Failed: $test"
    return "OK"
}
