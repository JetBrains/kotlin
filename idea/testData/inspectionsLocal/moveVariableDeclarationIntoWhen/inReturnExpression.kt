fun test() = 42

fun foo(): Int {
    val a<caret> = test()
    return when (a) {
        1 -> a
        else -> 24
    }
}