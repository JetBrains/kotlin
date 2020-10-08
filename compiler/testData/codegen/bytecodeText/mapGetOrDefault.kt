// IGNORE_BACKEND_FIR: JVM_IR
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

// The Kotlin version of getOrDefault, which redirects to the default implementation in java.util.Map
// 1 public bridge getOrDefault\(Ljava/lang/String;Ljava/lang/String;\)Ljava/lang/String;

// Test expectations differ between JVM and JVM IR backends, because of KT-40277. This should be revisited once KT-40277 is resolved.

// JVM_TEMPLATES:
// 1 public final bridge getOrDefault\(Ljava/lang/Object;Ljava/lang/Object;\)Ljava/lang/Object;

// JVM_IR_TEMPLATES:
// 1 public bridge getOrDefault\(Ljava/lang/String;Ljava/lang/String;\)Ljava/lang/String;
// 1 public final bridge getOrDefault\(Ljava/lang/Object;Ljava/lang/String;\)Ljava/lang/String;