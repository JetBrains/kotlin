// ISSUE: KT-90169
// Expected failure before the fix for KT-90169: "FAIL Array<Int>: 1"
// IGNORE_BACKEND: ANY
// WITH_STDLIB

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <E> countG(vararg xs: E): Int = xs.size

fun check(name: String, actual: Int, expected: Int = 2): String? =
    if (actual != expected) "FAIL $name: $actual" else null

fun box(): String {
    val arr = arrayOf("a", "b")
    return check("Array<Int>", applyIt(arrayOf(1, 2), ::countG))
        ?: check("Array<Any?>", applyIt(arrayOf<Any?>(1, null), ::countG))
        ?: check("Array<out String>", applyIt(arr as Array<out String>, ::countG))
        ?: check("empty array", applyIt(emptyArray<String>(), ::countG), expected = 0)
        ?: check("nested arrays", applyIt(arrayOf(arrayOf("a"), arrayOf("b")), ::countG))
        ?: "OK"
}
