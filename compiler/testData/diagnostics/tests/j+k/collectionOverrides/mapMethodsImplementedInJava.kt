// FIR_IDENTICAL
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-68362

// FILE: Base.java
import java.util.Map;

abstract class Base<T> implements Map<String, T> {
    @Override
    public abstract String get(Object key);
}

// FILE: Derived.java
import java.util.*;

public class Derived extends Base<String> {
    @Override
    public int size() { return 0; }
    @Override
    public boolean isEmpty() { return false; }
    @Override
    public boolean containsKey(Object key) { return false; }
    @Override
    public boolean containsValue(Object value) { return false; }
    @Override
    public String get(Object key) { return null; }
    @Override
    public String put(String key, String value) { return null; }
    @Override
    public String remove(Object key) { return null; }
    @Override
    public void putAll(Map<? extends String, ? extends String> m) {}
    @Override
    public void clear() {}
    @Override
    public Set<String> keySet() { return null; }
    @Override
    public Collection<String> values() { return null; }
    @Override
    public Set<Entry<String, String>> entrySet() { return null; }
}

// FILE: main.kt
class Impl : Derived()
