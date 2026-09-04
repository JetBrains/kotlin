// WITH_STDLIB
var calls = 0
fun pred(): (Int) -> Boolean { calls++; return { it == 3 } }
fun box(): String {
    val r = sequenceOf(1, 2, 3).indexOfFirst(pred())
    return if (r == 2 && calls == 1) "OK" else "Fail: r=$r calls=$calls"
}
