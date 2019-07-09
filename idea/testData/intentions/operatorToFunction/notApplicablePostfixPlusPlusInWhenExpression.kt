// IS_APPLICABLE: false
fun foo() {
    var a = 0
    val b = when {
        true -> a++<caret>
        else -> a--
    }
}
