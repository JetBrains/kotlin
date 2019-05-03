// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

// FILE: TestMap.java

import java.util.Collection;
import java.util.Map;
import java.util.Set;


public class TestMap implements Map<String, Object> {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Object get(Object key) {
        return null;
    }


    @Override
    public Object put(String key, Object value) {
        return null;
    }

    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {

    }

    @Override
    public void clear() {

    }


    @Override
    public Set<String> keySet() {
        return null;
    }


    @Override
    public Collection<Object> values() {
        return null;
    }


    @Override
    public Set<Entry<String, Object>> entrySet() {
        return null;
    }
}

// FILE: main.kt

class MyMap: TestMap()

fun box(): String {
    val map = MyMap()
    if (map.remove("aaa", 42)) return "fail 1"
    if (map.getOrDefault("aaa", 42) != 42) return "fail 2"

    return "OK"
}
