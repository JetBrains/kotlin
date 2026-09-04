// WITH_STDLIB
fun pred(): (Int) -> Boolean = { it == 2 }
fun box(): String {
    val r = sequenceOf(1, 2, 3).first(pred())
    return if (r == 2) "OK" else "Fail: $r"
}
