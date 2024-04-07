// ISSUE: KT-62118
// FILE: test.kt
class MyMutableEntry<K, V>(
    override val key: K, override var value: V
) : MutableMap.MutableEntry<K, V> {
    override fun setValue(newValue: V): V {
        value = newValue
        return value
    }
}

class MyImmutableEntry<K, V>(
    override val key: K, override val value: V
) : Map.Entry<K, V>

fun entries(map: HashMap<String, Int>) = map.entries
fun keys(map: HashMap<String, Int>) = map.keys
fun value(map: HashMap<String, Int>) = map.values

fun test() {
    val map = HashMap<String, Int>()
    entries(map).add(<!TYPE_MISMATCH, TYPE_MISMATCH!>MyMutableEntry(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)<!>)
    entries(map).add(MyMutableEntry("", 1))
    entries(map).add(<!TYPE_MISMATCH!>MyImmutableEntry("", 1)<!>)
    keys(map).add(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    value(map).add(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    J().setX(null)
    J().x = <!NULL_FOR_NONNULL_TYPE!>null<!>
}

open class K {
    var x: String = ""
}

// FILE: J.java
public class J extends K {
    @Override
    public void setX(String value) {}
}