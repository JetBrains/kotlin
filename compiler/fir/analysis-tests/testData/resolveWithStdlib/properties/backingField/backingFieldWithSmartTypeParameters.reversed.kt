val items: List<String>
    field = mutableListOf()

fun test() {
    items.<!UNRESOLVED_REFERENCE!>add<!>("one more item")
}
