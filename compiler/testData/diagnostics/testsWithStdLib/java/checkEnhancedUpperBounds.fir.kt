// FULL_JDK

// FILE: MapLike.java
import java.util.Map;

public interface MapLike<@org.jetbrains.annotations.NotNull K, V> {
    void putAll(Map<K, V> map);
}

// FILE: main.kt
fun test(map : MapLike<Int?, Int>) {}
