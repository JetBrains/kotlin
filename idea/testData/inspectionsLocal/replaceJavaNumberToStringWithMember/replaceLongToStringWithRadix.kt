// WITH_RUNTIME

fun foo() {
    var number = 5L
    val numberString = java.lang.Long.<caret>toString(number, 2)
}
