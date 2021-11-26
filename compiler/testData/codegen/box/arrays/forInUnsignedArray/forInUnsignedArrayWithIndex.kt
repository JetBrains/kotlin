// WITH_STDLIB

fun test(uis: UIntArray): String {
    var s = ""
    for ((i, ui) in uis.withIndex()) {
        s += "$i:$ui;"
    }
    return s
}


fun box(): String {
    val test = test(uintArrayOf(1U, 2U, 3U))
    if (test != "0:1;1:2;2:3;") return "Failed: $test"
    return "OK"
}