// ISSUE: KT-85139
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

fun positionBased() = { [a, b]: Pair<Char, Char> -> "$a$b" }('O' to 'K')
fun nameBased() = { (first , second): Pair<Char, Char> -> "$first$second" }('O' to 'K')
inline fun nameBasedRenamed() = { (a = first, b = second): Pair<Char, Char> -> "$a$b" }('O' to 'K')
fun nameBasedUnderscore() = { (_ = first, second): Pair<String, String> -> "$second" }("FAIL" to "OK")
fun nameBasedUnderscoreOnly() = { (_ = first, _ = second): Pair<String, String> -> "OK" }("FAIL" to "FAIL")

fun box(): String {
    val res = positionBased() + nameBased() + nameBasedRenamed() + nameBasedUnderscore() + nameBasedUnderscoreOnly()
    return if(res == "OKOKOKOKOK") "OK" else "FAIL: $res"
}
