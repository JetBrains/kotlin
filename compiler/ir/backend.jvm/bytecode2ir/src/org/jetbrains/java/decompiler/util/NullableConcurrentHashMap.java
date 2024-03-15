package org.jetbrains.java.decompiler.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// This is a non-compliant Map implementation. Use at your own mortal peril.
// It's also a incomplete implementation. More methods will be added as needed.
public class NullableConcurrentHashMap<K,V> extends ConcurrentHashMap<K,V> {
  private final Object NULL_KEY = new Object();
  private final Object NULL_VALUE = new Object();

  @Override
  public V get(Object key) {
    if (key == null) {
      key = NULL_KEY;
    }

    V res = super.get(key);
    if (res == NULL_VALUE) {
      return null;
    }

    return res;
  }

  @Override
  public V put(K key, V value) {
    if (key == null) {
      key = (K) NULL_KEY;
    }

    if (value == null) {
      value = (V) NULL_VALUE;
    }

    return super.put(key, value);
  }

  @Override
  public boolean containsKey(Object key) {
    if (key == null) {
      key = NULL_KEY;
    }

    return super.containsKey(key);
  }

  @Override
  public V remove(Object key) {
    if (key == null) {
      key = NULL_KEY;
    }

    return super.remove(key);
  }

  // entrySet does NOT return a backing set. Any modifications to this set will be LOST.
  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> ourSet = super.entrySet();
    Set<Entry<K, V>> entries = new HashSet<>(ourSet);

    for (Entry<K, V> entry : ourSet) {
      K key = entry.getKey();
      V value = entry.getValue();

      boolean mod = false;
      if (key == NULL_KEY) {
        key = null;
        mod = true;
      }

      if (value == NULL_VALUE) {
        value = null;
        mod = true;
      }

      if (mod) {
        entries.remove(entry);
        entries.add(new SimpleEntry(key, value));
      }
    }

    return entries;
  }

  private static final class SimpleEntry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private final V value;

    public SimpleEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }
  }
}
