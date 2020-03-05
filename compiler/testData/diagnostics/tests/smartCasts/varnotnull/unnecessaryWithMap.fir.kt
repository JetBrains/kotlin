// !WITH_NEW_INFERENCE

fun create(): Map<String, String> = null!!

operator fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>> = null!!

operator fun <K, V> Map.Entry<K, V>.component1() = key

operator fun <K, V> Map.Entry<K, V>.component2() = value

class MyClass {
    private var m: Map<String, String>? = null
    fun foo(): Int {
        var res = 0
        m = create()
        // See KT-7428
        <!INAPPLICABLE_CANDIDATE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for ((k, v) in m)
            <!UNRESOLVED_REFERENCE!>res += (k.<!UNRESOLVED_REFERENCE!>length<!> + v.<!UNRESOLVED_REFERENCE!>length<!>)<!><!>
        return res
    }
}