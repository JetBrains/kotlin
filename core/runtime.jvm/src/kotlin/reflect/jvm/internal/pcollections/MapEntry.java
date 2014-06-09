package kotlin.reflect.jvm.internal.pcollections;

/**
 * <p/>
 * An Entry maintaining an immutable key and value.  This class
 * does not support method <tt>setValue</tt>.  This class may be
 * convenient in methods that return thread-safe snapshots of
 * key-value mappings.
 */
final class MapEntry<K, V> implements java.io.Serializable {
    private static final long serialVersionUID = 7138329143949025153L;

    private final K key;
    private final V value;

    public MapEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapEntry)) return false;
        MapEntry<?, ?> e = (MapEntry<?, ?>) o;
        return (key == null ? e.key == null : key.equals(e.key)) &&
                (value == null ? e.value == null : value.equals(e.value));
    }

    @Override
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) ^
                (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
