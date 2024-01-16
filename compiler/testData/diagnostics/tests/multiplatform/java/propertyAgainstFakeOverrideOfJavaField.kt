// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect class MyHashMap<K, V> {
    val values: Collection<V>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface MyMap<K, V> {
    val values: Collection<V>
}

actual typealias MyHashMap<K, V> = MyHashMapJava<K, V>

// FILE: MyHashMapJava.java
import java.util.Collection;

public final class MyHashMapJava<K, V> extends AbstractMyHashMap<K, V> {
    // fake override of field AbstractMyHashMap.values
}
abstract class AbstractMyHashMap<K, V> implements MyMap<K, V> {
    protected Collection<V> values;

    @Override
    public Collection<V> getValues() {
        return values;
    }
}
