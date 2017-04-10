package test

// Tests that type variables of properties are written to the getter signature

val <K, V> Map<K, V>.test: Map<V, K>
    get() = this as Map<V, K>
