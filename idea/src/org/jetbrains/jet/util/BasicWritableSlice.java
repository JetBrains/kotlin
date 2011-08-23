package org.jetbrains.jet.util;

/**
* @author abreslav
*/
public class BasicWritableSlice<K, V> implements WritableSlice<K,V> {

    private final String debugName;
    private final RewritePolicy rewritePolicy;

    public BasicWritableSlice(String debugName, RewritePolicy rewritePolicy) {
        this.debugName = debugName;
        this.rewritePolicy = rewritePolicy;
    }

    @Override
    public ManyMapKey<K, V> makeKey(K key) {
        return new ManyMapKey<K, V>(this, key);
    }

    // True to put, false to skip
    @Override
    public boolean check(K key, V value) {
        assert key != null;
        assert value != null;
        return true;
    }

    @Override
    public void afterPut(MutableManyMap manyMap, K key, V value) {
        // Do nothing
    }

    @Override
    public V computeValue(ManyMap map, K key, V value, boolean valueNotFound) {
        if (valueNotFound) assert value == null;
        return value;
    }


    @Override
    public RewritePolicy getRewritePolicy() {
        return rewritePolicy;
    }

    @Override
    public String toString() {
        return debugName;
    }
}
