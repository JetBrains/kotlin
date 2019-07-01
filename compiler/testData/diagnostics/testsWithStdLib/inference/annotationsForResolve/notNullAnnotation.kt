// !LANGUAGE: +NewInference

// FILE: SmartFMap.java

public class SmartFMap<K, V> implements java.util.Map<K, V> {
    public static < K, V> SmartFMap<K, V> emptyMap() {
        return null;
    }

    public SmartFMap < K, V> plus(@org.jetbrains.annotations.NotNull K key, V value) {
        return null;
    }
}

// FILE: main.kt

class KotlinType

interface TypePredicate : (KotlinType) -> Boolean {
    override fun invoke(typeToCheck: KotlinType): Boolean
}

fun <T : Any?> TypePredicate.expectedTypeFor(keys: Iterable<T>): Map<T, TypePredicate> =
    keys.fold(SmartFMap.emptyMap<T, TypePredicate>()) { map, key ->
        map.plus(key, this)
    }
