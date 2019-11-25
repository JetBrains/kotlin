fun test_1() {
    val comp = <!UNRESOLVED_REFERENCE!>Comparator<!><Int> { x, y -> 1 }
}

fun test_2(list: List<Int>) {
    val comp = java.util.<!UNRESOLVED_REFERENCE!>Comparator<!><Int> { x, y -> 1 }
}

fun test_3(comparator: java.util.Comparator<Int>) {
    comparator.compare(1, 2)
}

fun test_4(comparator: Comparator<Int>) {
    comparator.compare(1, 2)
}