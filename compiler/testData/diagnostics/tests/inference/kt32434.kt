// FULL_JDK
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// Issue: KT-32434

// FILE: CacheMonoJava.java

public class CacheMonoJava {
    public static <K, V> Mono<V> lookup(java.util.Map<K, ? super Signal<? extends V>> map, K key) {
        throw new UnsupportedOperationException();
    }
}

// FILE: main.kt

interface Cache<K, V> {
    fun asMap(): MutableMap<K, V>
}

interface Mono<E>
interface Signal<E> : Mono<E>

interface AttributeDefinition

val cache: Cache<String, Signal<out AttributeDefinition>> = TODO()

object CacheMonoKotlin {
    fun <K, V> lookup(map: MutableMap<K, in Signal<out V>>, key: K): Mono<V> = TODO()
}

fun findByName_java(name: String): Mono<AttributeDefinition> = CacheMonoJava.lookup(cache.asMap(), name)
fun findByName_kotlin(name: String): Mono<AttributeDefinition> = CacheMonoKotlin.lookup(cache.asMap(), name)
