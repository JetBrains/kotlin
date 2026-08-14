// WITH_STDLIB

private inline fun scan(n: Int, charAt: (Int) -> Char): Int {
    var acc = 0
    var i = 0
    while (i < n) {
        var c = charAt(i).code
        acc += run { c = charAt(i).code; c }
        i++
    }
    return acc
}

private fun test(s: String): Int = run { scan(s.length, s::get) }

fun box(): String {
    val result = test("OK")
    val expected = 'O'.code + 'K'.code
    return if (result == expected) "OK" else "fail: $result"
}
