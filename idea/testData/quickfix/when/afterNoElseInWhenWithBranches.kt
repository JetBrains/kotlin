// "Add else branch" "true"
fun test() {
    val a = 12
    when (a) {
        in 0..11 -> { /* some code */ }
        12, 13, 14 -> { /* some code */ }
        else -> {<caret>
        }
    }
}