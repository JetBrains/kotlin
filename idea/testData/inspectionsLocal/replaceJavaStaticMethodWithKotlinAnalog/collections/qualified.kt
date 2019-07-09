// WITH_RUNTIME
fun test() {
    val array = intArrayOf(1, 2, 3)
    val result = java.util.Arrays.<caret>copyOf(array, 3)
}