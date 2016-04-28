package test;
import java.util.Map;
public class MapRemove {
    public abstract class MyMap<K, V> implements Map<K, V> {
        public boolean remove(Object x, Object y) { return false; }
    }

    public abstract class MyMapString implements Map<String, Integer> {
        public boolean remove(Object x, Object y) { return false; }
    }

    public abstract class MyMapStringInvalid implements Map<String, Integer> {
        public boolean remove(String x, Integer y) { return false; }
    }
}
