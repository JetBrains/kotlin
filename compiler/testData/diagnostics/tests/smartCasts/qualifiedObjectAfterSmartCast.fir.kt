// ISSUE: KT-69191

object None {
    object Type
}

fun <V> typeOf(value: V): Any {
    return when (value) {
        None -> None.<!UNRESOLVED_REFERENCE!>Type<!>
        else -> ""
    }
}
