fun basic() {
    val lst = listOf(1, 2)
    val mLst = linkedListOf(1, 2, 3)
    42
}

fun listWithTwoPossibleSizes(cond: Boolean) {
    val lst: List<Boolean>
    if (cond) {
        lst = listOf(false, false)
    }
    else {
        lst = linkedListOf(false, true, true)
    }
    42
}