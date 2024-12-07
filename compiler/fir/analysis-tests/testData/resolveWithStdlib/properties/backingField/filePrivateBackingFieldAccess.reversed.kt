// RUN_PIPELINE_TILL: BACKEND
val list: List<String>
    field = mutableListOf<String>()

fun add(s: String) {
    list.<!UNRESOLVED_REFERENCE!>add<!>(s)
}
