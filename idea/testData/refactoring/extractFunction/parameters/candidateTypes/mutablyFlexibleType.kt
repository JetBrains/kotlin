// WITH_RUNTIME
// PARAM_DESCRIPTOR: val data: (kotlin.collections.MutableList<(String..String?)>..kotlin.collections.List<(String..String?)>) defined in test
// PARAM_TYPES: kotlin.collections.List<(String..String?)>, kotlin.collections.MutableList<(String..String?)>, kotlin.collections.MutableCollection<(String..String?)>, kotlin.collections.Collection<(String..String?)>
fun test(): Boolean {
    val j: J? = null
    val data = j?.getData() ?: return false
    return <selection>data.contains("foo")</selection>
}