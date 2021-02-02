// !LANGUAGE: +ImprovementsAroundTypeEnhancement
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FULL_JDK

// FILE: MapLike.java
import java.util.Map;

public interface MapLike<@org.jetbrains.annotations.NotNull K, V> {
    void putAll(Map<K, V> map);
}

// FILE: main.kt
fun test0(map : MapLike<<!UPPER_BOUND_VIOLATED!>Int?<!>, Int>) {}
fun <K> test11(map : MapLike<<!UPPER_BOUND_VIOLATED!>K<!>, K>) {}
fun <K> test12(map : MapLike<<!UPPER_BOUND_VIOLATED!>K?<!>, K>) {}
fun <K : Any> test13(map : MapLike<K, K>) {}
fun <K : Any> test14(map : MapLike<<!UPPER_BOUND_VIOLATED!>K?<!>, K>) {}
