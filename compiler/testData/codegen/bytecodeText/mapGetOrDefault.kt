// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
// FILE: TestMap.java

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class TestMap<K, V> implements Map<K, V> {
    public void clear() {}
    public boolean isEmpty() { return true; }
    public int size() { return 0; }
    public boolean containsKey(Object key) { return false; }
    public boolean containsValue(Object value) { return false; }
    public V get(Object key) { return null; }
    public V put(K key, V value) { return null; }
    public V remove(Object key) { return null; }
    public void putAll(Map<? extends K, ? extends V> m) {}
    public Set<K> keySet() { return Collections.EMPTY_SET; }
    public Collection<V> values() {return Collections.EMPTY_SET; }
    public Set<Map.Entry<K, V>> entrySet() { return Collections.EMPTY_SET; }
}

// FILE: main.kt

class MyMap: TestMap<String, String>()

// 1 public final bridge getOrDefault\(Ljava/lang/Object;Ljava/lang/Object;\)Ljava/lang/Object;
