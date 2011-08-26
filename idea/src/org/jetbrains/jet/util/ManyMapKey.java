package org.jetbrains.jet.util;

import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public final class ManyMapKey<K, V> {

    private final WritableSlice<K, V> slice;
    private final K key;

    public ManyMapKey(@NotNull WritableSlice<K, V> slice, K key) {
        this.slice = slice;
        this.key = key;
    }

    public WritableSlice<K, V> getSlice() {
        return slice;
    }

    public K getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ManyMapKey that = (ManyMapKey) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (!slice.equals(that.slice)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = slice.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return slice + " -> " + key;
    }
}
