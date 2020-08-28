fun downUnder(): Int = 4
fun downParameter<caret>(p: Int): Int {
    "first $p"
    "second $p"
    p
    return p
}
fun callDown() {
    val result = downParameter(downUnder())
}