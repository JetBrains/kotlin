// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: A.java
public class A<K, V> {
    public void foo(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {

    }
}

// FILE: BiFunction.java
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
}

// FILE: main.kt
fun main() {
    val a = A<Int, String>()
    a.foo(2, BiFunction { k, v -> null })
    a.foo(2) { k, v -> null } // See KT-12144
}
