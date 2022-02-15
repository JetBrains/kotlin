// ISSUE: KT-51274

fun test() {
    val x = <!UNRESOLVED_REFERENCE!>unresolved<!>()
    val y = when (x) {
        is String -> x
        else -> throw Exception()
    }
}
