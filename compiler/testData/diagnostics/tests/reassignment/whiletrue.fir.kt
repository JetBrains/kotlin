// !DIAGNOSTICS: -UNUSED_VALUE

fun foo(): Int {
    val i: Int
    var j = 0
    while (true) {
        i = ++j
        if (j > 5) break
    } 
    return i
}
