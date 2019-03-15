// IS_APPLICABLE: false
fun foo() {
    var a = 0
    val b = if (false) {
        if (true) {
            a++<caret>
        } else a
    } else a
}
