import java.lang.*;
import java.util.*;

public class Test {
    public static class IterableImpl implements Iterable<String> {
        public Iterator<String> iterator() { return new IteratorImpl(); }
    }

    public static class IteratorImpl implements Iterator<String> {
        public boolean hasNext() { return false; }
        public String next() { return null; }
        public void remove() { }
    }

    public static class MapEntryImpl implements Map.Entry<String, String> {
        public String getKey() { return null; }
        public String getValue() { return null; }
        public String setValue(String s) { return null; }
    }
}
