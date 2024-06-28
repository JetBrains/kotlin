// FULL_JDK
// SCOPE_DUMP: SomeMap:containsKey;containsValue;get;remove, MyMap:containsKey;containsValue;get

// FILE: MyBaseMap.java
public interface MyBaseMap<K1, V1> {
    boolean containsKey(Object key);
}

// FILE: MyMap.java
public interface MyMap<K2, V2> extends MyBaseMap<K2, V2> {
    @Override
    boolean containsKey(Object key);
    boolean containsValue(Object key);

    V2 get(K2 key);
}

// FILE: SomeMap.java
import java.util.Map;

public abstract class SomeMap<K3, V3> implements Map<K3, V3>, MyMap<K3, V3> {
    @Override
    public abstract boolean containsKey(Object key);

    @Override
    public abstract V3 remove(Object key);
}

// FILE: main.kt

fun test(map: SomeMap<Int, String>) {
    map.containsKey(1) // ok
    map.containsKey(<!ARGUMENT_TYPE_MISMATCH!>""<!>) // error

    map.containsValue("") // ok
    map.containsValue(<!ARGUMENT_TYPE_MISMATCH!>1<!>) // error

    map.get(1) // ok
    map.get(<!ARGUMENT_TYPE_MISMATCH!>""<!>) // error

    map.remove(1) // ok
    map.remove(<!ARGUMENT_TYPE_MISMATCH!>""<!>) // error
}
