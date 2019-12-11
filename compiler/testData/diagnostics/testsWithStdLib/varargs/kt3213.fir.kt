// !CHECK_TYPE

fun test(a: Array<out String>) {
    val b = a.toList()

    b checkType { <!UNRESOLVED_REFERENCE!>_<!><List<String>>() }
}