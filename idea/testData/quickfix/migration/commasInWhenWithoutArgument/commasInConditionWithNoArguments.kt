// "Replace ',' with '||' in when" "true"
fun test(i: Int, j: Int) {
    var b = false
    when {
        i > 0<caret>, j > 0 -> { /* some code 1 */ }
        i < 0, j < 0 -> { /* some code 2 */ }
        else -> { /* other code */ }
    }
}