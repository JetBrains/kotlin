// PROBLEM: none
fun test() = true

fun foo(): Int {
    val a<caret> = test()
    return null ?: when (a) {
        true -> 42
        else -> 5
    }
}