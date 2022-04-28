// WITH_STDLIB
// ISSUE: KT-52197

fun <K, V> helper(builderAction: MutableMap<K, V>.() -> Unit) {
    builderAction(mutableMapOf())
}

fun test(){
    helper {
        val x = put("key", "value")
        if (x != null) {
            "Error: $x"
            x.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>length<!>
        }
        x.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>length<!>
    }
}
