// FILE: Another.kt
fun another() = it2()

// FILE: InlineTarget.kt
inline fun it1() = "I am foo"

fun it2() = "I am bar"

// FILE: main.kt
fun callInlineTarget() = it1()