package test;

import java.util.*;

public interface SubclassOfMapEntry<K, V> extends Map.Entry<K, V> {
    V setValue(V value);
}
