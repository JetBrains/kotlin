// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: box.kt

class MyMap<K, V> : JImpl<K, V>()

fun box(): String {
    val a = MyMap<Int, String>()
    a.put(42, "OK")
    return a.entries.iterator().next().value
}

// FILE: J.java

import java.util.*;

public interface J<K, V> extends Map<K, V> {
    @Override
    default Set<Entry<K, V>> entrySet() {
        return myEntrySet();
    }

    Set<Entry<K,V>> myEntrySet();
}

// FILE: JImpl.java

import java.util.*;

public class JImpl<K, V> implements J<K, V> {
    private final Map<K, V> delegate = new HashMap<>();

    @Override
    public Set<Entry<K, V>> myEntrySet() {
        return delegate.entrySet();
    }
    @Override
    public int size() {
        return delegate.size();
    }
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }
    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }
    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }
    @Override
    public V get(Object key) {
        return delegate.get(key);
    }
    @Override
    public V put(K key, V value) {
        return delegate.put(key, value);
    }
    @Override
    public V remove(Object key) {
        return delegate.remove(key);
    }
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        delegate.putAll(m);
    }
    @Override
    public void clear() {
        delegate.clear();
    }
    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }
    @Override
    public Collection<V> values() {
        return delegate.values();
    }
}
