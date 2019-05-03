fun test() = true

fun foo() {
    val a<caret> = test()
    val b = !when (a) {
        true -> true
        else -> false
    }
}