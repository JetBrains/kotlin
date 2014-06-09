package kotlin.reflect.jvm.internal.pcollections;

/**
 * An immutable, persistent map from non-null keys of type K to non-null values of type V.
 *
 * @author harold
 */
public interface PMap<K, V> {
    public PMap<K, V> plus(K key, V value);

    public PMap<K, V> minus(Object key);

    V get(Object key);
}
