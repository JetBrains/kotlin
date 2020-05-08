// WITH_RUNTIME
fun test(array: IntArray) {
    val b = array.all<caret> { it != 1 }
}