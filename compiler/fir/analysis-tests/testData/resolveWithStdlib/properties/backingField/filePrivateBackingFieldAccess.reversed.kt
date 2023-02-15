val list: List<String>
    field = mutableListOf<String>()

fun add(s: String) {
    list.<!UNRESOLVED_REFERENCE!>add<!>(s)
}
