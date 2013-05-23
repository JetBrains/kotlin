// IS_APPLICABLE: false
// MOVE: down

fun foo(n: Int) {
    when (n) {
        0 -> {

        }
        1 -> {

        }<caret>
        else -> {

        }
    }
    val x = ""
}