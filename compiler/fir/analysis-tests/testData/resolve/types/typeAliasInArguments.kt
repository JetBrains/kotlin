// FILE: MyMap.java

public interface MyMap<K, V> {
    public MyMap<K, V> put(K k, V v);
}
// FILE: MySet.java

public interface MySet<E> {
    public MySet<E> add(E e);
}

// FILE: main.kt

typealias ImmutableMap<K, V> = MyMap<K, V>
typealias ImmutableSet<E> = MySet<E>

private typealias ImmutableMultimap<K, V> = ImmutableMap<K, ImmutableSet<V>>

private fun <K, V> ImmutableMultimap<K, V>.put(key: K, value: V, oldSet: ImmutableSet<V>): ImmutableMultimap<K, V> {
    return put(key, oldSet.add(value))
}
