// IS_APPLICABLE: false
fun test(x: String?) {
    if (x <caret>!= null) {
        x.length
    }
}
