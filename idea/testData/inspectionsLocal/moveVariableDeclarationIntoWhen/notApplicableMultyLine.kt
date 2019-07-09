// PROBLEM: none

fun foo(): Int {
    val a<caret> = if (true) true
    else false

    return when (a) {
        true -> 42
        else -> null
    } ?: 55
}