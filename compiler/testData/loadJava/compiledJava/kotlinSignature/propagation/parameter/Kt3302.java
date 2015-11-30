package test;

import org.jetbrains.annotations.NotNull;

// See SubclassFromGenericAndNot, as well
public interface Kt3302 {
    public interface BSONObject {
        Object put(@NotNull String s, @NotNull Object o);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface LinkedHashMap<K, V> {
        public V put(K key, V value);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface BasicBSONObject extends LinkedHashMap<String, Object>, BSONObject {
        @Override
        public Object put(String key, Object value);
    }
}
