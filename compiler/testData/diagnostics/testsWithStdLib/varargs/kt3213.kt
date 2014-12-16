// !CHECK_TYPE

fun test(a: Array<out String>) {
    val b = a.toList()

    b checkType { it : _<List<String>> }
}