// Issue: KT-37736

internal class Z<K> {
    val map = HashMap<String, String>()
    inline fun compute(key: String, producer: () -> String): String {
        return map.getOrPut(key, ::<!UNSUPPORTED!>producer<!>)
    }
}
