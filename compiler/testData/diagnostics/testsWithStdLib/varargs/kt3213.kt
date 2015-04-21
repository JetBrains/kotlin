// !CHECK_TYPE

fun test(a: Array<out String>) {
    val b = a.toList()

    b checkType { _<List<String>>() }
}