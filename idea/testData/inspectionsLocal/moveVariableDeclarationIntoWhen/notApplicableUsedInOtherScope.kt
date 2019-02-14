// PROBLEM: none
fun foo() {
    val a<caret> = 1
    when (a) {
        1 -> {
        }
        else -> {
        }
    }
    val b = a
}