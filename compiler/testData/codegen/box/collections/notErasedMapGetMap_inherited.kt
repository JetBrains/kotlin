// TARGET_BACKEND: JVM_IR
// DUMP_IR
// DUMP_EXTERNAL_CLASS: MyMap
// ISSUE: KT-72345

// FILE: MyMapInterface.java
import java.util.*;

public interface MyMapInterface<V> extends Map<String, V> {
    @Override
    default V get(Object key) { return null; }
    @Override
    default int size() { return 0; }
    @Override
    default boolean isEmpty() { return false; }
    @Override
    default boolean containsKey(Object key) { return false; }
    @Override
    default boolean containsValue(Object value) { return false; }
    @Override
    default V put(String key, V value) { return null; }
    @Override
    default V remove(Object key) { return null; }
    @Override
    default void putAll(Map<? extends String, ? extends V> m) {}
    @Override
    default void clear() {}
    @Override
    default Set<String> keySet() { return null; }
    @Override
    default Collection<V> values() { return null; }
    @Override
    default Set<Entry<String, V>> entrySet() { return null; }
}

// FILE: NotMap.java
public class NotMap<V> {
    public V get(String key) {
        throw new RuntimeException("OK");
    }
}

// FILE: MyMap.java
public class MyMap<V> extends NotMap<V> implements MyMapInterface<V> {}


// FILE: main.kt
fun box(): String {
    val test2 = MyMap<String>()
    return try {
        "fail" + test2.get("test")!!
    } catch (e: RuntimeException) {
        e.message!!
    }
}
