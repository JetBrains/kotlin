fun test() = 42

fun foo() {
    val a<caret> = test()
    when (a) {
        1 -> a
        else -> 24
    }
}