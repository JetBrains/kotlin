fun test() = true

fun foo(): Int {
    val a<caret> = test()
    return when (a) {
        true -> 42
        else -> null
    } ?: 55
}