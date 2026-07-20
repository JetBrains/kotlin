// WITH_STDLIB
var calls = 0
fun seed(): Int { calls++; return 1 }
fun box(): String {
    val s = generateSequence(seed()) { if (it < 2) it + 1 else null }
    var sum = 0
    for (x in s) sum += x
    for (x in s) sum += x
    return if (calls == 1 && sum == 6) "OK" else "Fail: calls=$calls sum=$sum"
}
