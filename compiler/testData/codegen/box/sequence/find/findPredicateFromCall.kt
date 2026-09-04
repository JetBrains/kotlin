// WITH_STDLIB
fun pred(): (Int) -> Boolean = { it > 5 }
fun box(): String {
    val r = sequenceOf(1, 2, 3).find(pred())
    return if (r == null) "OK" else "Fail: $r"
}
