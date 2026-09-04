// WITH_STDLIB
var calls = 0
fun n(): Int { calls++; return 2 }
fun box(): String {
    val s = sequenceOf(1, 2, 3).take(n())
    var sum = 0
    for (x in s) sum += x
    for (x in s) sum += x
    return if (calls == 1 && sum == 6) "OK" else "Fail: calls=$calls sum=$sum"
}
