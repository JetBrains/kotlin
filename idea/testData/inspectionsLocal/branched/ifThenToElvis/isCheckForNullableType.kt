// PROBLEM: none

fun foo(x: CharSequence?) {
    val y = if (x is String?) {
        x
    }
    else {
        (x as CharSequence).toString()
    }<caret>
}
