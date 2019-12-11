// FILE: MyFunc.java
public interface MyFunc<K, V> {
    V apply(K String);
}

// FILE: ConcMap.java
public interface ConcMap<K, V> {
    V computeIfAbsent(K key, MyFunc<? super K,? extends V> mappingFunction);
}

// FILE: ConcHashMap.java
public class ConcHashMap<K, V> implements ConcMap<K, V> {
    @Override
    V computeIfAbsent(K key, MyFunc<? super K,? extends V> mappingFunction) { }
}

// FILE: main.kt

public fun concurrentMap() {
    val map = ConcHashMap<String, String>()
    map.computeIfAbsent("") { "" } // here
}