fun usage(arr: IntArray) {
    val array: IntArray
    for (i in array.indices) {
        method(array[i])
    }
}

fun <caret>method(i: Int) {
    println(i)
}
