// WITH_STDLIB

fun <A> rightTypes(rightType1: List<Int>, rightType2: List<A>) {
    rightType<caret_1_right>1
    rightType<caret_2_right>2
}

fun <B> leftTypes(leftType1: List<B>, leftType2: List<Int>) {
    leftType<caret_1_left>1
    leftType<caret_2_left>2
}
