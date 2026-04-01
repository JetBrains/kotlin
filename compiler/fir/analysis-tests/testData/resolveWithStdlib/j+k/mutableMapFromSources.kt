// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE
// ISSUE: KT-82772
// FILE: usage.kt
package usage

fun baz(map: PersistentMap<String, Int>) {
    map.put("", 1)
}

// FILE: PersistentMap.kt
package usage

interface PersistentMap<K, out V> : Map<K, V> {
    fun put(key: K, value: @UnsafeVariance V): PersistentMap<K, V>
}

// FILE: MutableMap.kt
package kotlin.collections

public interface MutableMap<K, V> : Map<K, V> {
    public fun put(key: K, value: V): V?

    public interface MutableEntry<K, V> : Map.Entry<K, V> {
        public fun setValue(newValue: V): V
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, interfaceDeclaration, nestedClass, nullableType, out,
stringLiteral, typeParameter */
