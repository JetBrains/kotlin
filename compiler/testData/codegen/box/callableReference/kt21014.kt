// WITH_STDLIB

fun box(): String {
    val ints = intArrayOf(1, 2, 3)

    val test1 = IntArray::size.get(ints)
    if (test1 != 3) throw Exception("IntArray::size.get(ints) != 3: $test1")

    val test2 = with(ints, IntArray::size)
    if (test2 != 3) throw Exception("with(ints, IntArray::size) != 3: $test2")

    return "OK"
}
