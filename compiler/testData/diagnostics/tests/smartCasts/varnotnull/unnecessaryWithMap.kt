fun create(): Map<String, String> = null!!

fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>> = null!!

fun <K, V> Map.Entry<K, V>.component1() = getKey()

fun <K, V> Map.Entry<K, V>.component2() = getValue()

class MyClass {
    private var m: Map<String, String>? = null
    fun foo(): Int {
        var res = 0
        m = create()
        // See KT-7428
        for ((k, v) in <!SMARTCAST_IMPOSSIBLE!>m<!>)
            res += (k.length() + v.length())
        return res
    }
}