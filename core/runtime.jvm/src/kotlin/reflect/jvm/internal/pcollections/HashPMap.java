package kotlin.reflect.jvm.internal.pcollections;

import static java.util.Map.Entry;

/**
 * A persistent map from non-null keys to non-null values.
 * <p/>
 * This map uses a given integer map to map hashcodes to lists of elements
 * with the same hashcode. Thus if all elements have the same hashcode, performance
 * is reduced to that of an association list.
 * <p/>
 * This implementation is thread-safe, although its iterators may not be.
 *
 * @author harold
 */
public final class HashPMap<K, V> implements PMap<K, V> {
    public static final HashPMap<Object, Object> EMPTY = new HashPMap<Object, Object>(IntTreePMap.<PStack<Entry<Object, Object>>>empty(), 0);

    @SuppressWarnings("unchecked")
    public static <K, V> HashPMap<K, V> empty() {
        return (HashPMap<K, V>) HashPMap.EMPTY;
    }

    private final IntTreePMap<PStack<Entry<K, V>>> intMap;
    private final int size;

    private HashPMap(IntTreePMap<PStack<Entry<K, V>>> intMap, int size) {
        this.intMap = intMap;
        this.size = size;
    }

    public int size() {
        return size;
    }

    public boolean containsKey(Object key) {
        return keyIndexIn(getEntries(key.hashCode()), key) != -1;
    }

    @Override
    public V get(Object key) {
        PStack<Entry<K, V>> entries = getEntries(key.hashCode());
        for (Entry<K, V> entry : entries)
            if (entry.getKey().equals(key))
                return entry.getValue();
        return null;
    }

    @Override
    public HashPMap<K, V> plus(K key, V value) {
        PStack<Entry<K, V>> entries = getEntries(key.hashCode());
        int size0 = entries.size();
        int i = keyIndexIn(entries, key);
        if (i != -1) entries = entries.minus(i);
        entries = entries.plus(new SimpleImmutableEntry<K, V>(key, value));
        return new HashPMap<K, V>(intMap.plus(key.hashCode(), entries), size - size0 + entries.size());
    }

    @Override
    public HashPMap<K, V> minus(Object key) {
        PStack<Entry<K, V>> entries = getEntries(key.hashCode());
        int i = keyIndexIn(entries, key);
        if (i == -1) // key not in this
            return this;
        entries = entries.minus(i);
        if (entries.size() == 0) // get rid of the entire hash entry
            return new HashPMap<K, V>(intMap.minus(key.hashCode()), size - 1);
        // otherwise replace hash entry with new smaller one:
        return new HashPMap<K, V>(intMap.plus(key.hashCode(), entries), size - 1);
    }

    private PStack<Entry<K, V>> getEntries(int hash) {
        PStack<Entry<K, V>> entries = intMap.get(hash);
        if (entries == null) return ConsPStack.empty();
        return entries;
    }

    private static <K, V> int keyIndexIn(PStack<Entry<K, V>> entries, Object key) {
        int i = 0;
        for (Entry<K, V> entry : entries) {
            if (entry.getKey().equals(key))
                return i;
            i++;
        }
        return -1;
    }
}
