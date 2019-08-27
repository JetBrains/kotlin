// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

// FILE: Inv2.java

public class Inv2<K, V> {}

// FILE: JavaSet.java

public class JavaSet {
    public static <E> java.util.Set<E> newIdentityHashSet() { return null; }

    public static <K, V> V get(Inv2<K, V> slice, K key) { return null; }
}

// FILE: test.kt

fun <K> select(x: K, y: K): K = x

fun <K, T> addElementToSlice(
    slice: Inv2<K, MutableCollection<T>>,
    key: K,
    element: T
) {
    val a = select(JavaSet.get(slice, key), JavaSet.newIdentityHashSet())

    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.collections.MutableCollection<T>..kotlin.collections.Collection<T>?)")!>a<!>

    a.add(element)
}