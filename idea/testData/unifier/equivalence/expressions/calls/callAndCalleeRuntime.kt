fun test(index: Int, array: Array<() -> Unit>) {
    val a = <selection>array[index]</selection>()
    val b = array[index]
    val c = array.get(index)
    val d = array.get(index).invoke()
}