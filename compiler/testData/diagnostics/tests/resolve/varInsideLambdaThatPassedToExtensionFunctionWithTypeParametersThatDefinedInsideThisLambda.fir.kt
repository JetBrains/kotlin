// RUN_PIPELINE_TILL: FRONTEND
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
            x.length
        }
        x<!UNSAFE_CALL!>.<!>length
    }
}
