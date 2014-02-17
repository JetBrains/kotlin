// Tests that type variables of properties are written to the getter signature

package a

val <K, V> test: Map<K, V>
    get() = java.util.HashMap()
