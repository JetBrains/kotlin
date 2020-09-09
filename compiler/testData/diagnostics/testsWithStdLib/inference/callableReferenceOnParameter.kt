// Issue: KT-37736

internal class Z<K> {
    val map = HashMap<String, String>()
    inline fun compute(key: String, <!UNUSED_PARAMETER!>producer<!>: () -> String): String {
        return map.getOrPut(key, ::<!UNSUPPORTED, USAGE_IS_NOT_INLINABLE!>producer<!>)
    }
}