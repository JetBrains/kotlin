// WITH_STDLIB
var calls = 0
fun mkSeed(): () -> Int? { calls++; return { 1 } }
fun box(): String {
    val s = generateSequence(mkSeed()) { it + 1 }
    val a = s.first()
    val b = s.first()
    return if (a == 1 && b == 1 && calls == 1) "OK" else "Fail: $a, $b, calls=$calls"
}
