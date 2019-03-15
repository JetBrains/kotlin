// WITH_RUNTIME
fun test(b: Boolean, x: Int, y: Int?) {
    val list = mutableListOf<Int?>()
    <caret>if (b) {
        list += x
    } else {
        list += y
    }
}