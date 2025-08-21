inline fun calculate(): Int {
    val smallNumber = {
        1
    }
    val bigNumber = {
        40 + smallNumber()
    }
    return smallNumber() + bigNumber()
}
