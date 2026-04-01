// ISSUE: KT-85139
// WITH_STDLIB

fun box(): String = { (a, b): Pair<Char, Char> -> "$a$b" }('O' to 'K')
