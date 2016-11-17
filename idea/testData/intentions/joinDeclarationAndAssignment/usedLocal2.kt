// IS_APPLICABLE: false
fun foo() {
    // It's possible here to join with assignment but move the result after declaration of i
    var b<caret>: Int
    val i = 0
    b = i
}
