// FILE: GenericProperty.java
package test;

class GenericProperty {
    void foo() {
        java.util.Map<String, Integer> o = GenericPropertyKt.getTest(new java.util.HashMap<Integer, String>());
    }
}

// FILE: GenericProperty.kt
package test

// Tests that type variables of properties are written to the getter signature

val <K, V> Map<K, V>.test: Map<V, K>
    get() = this as Map<V, K>
