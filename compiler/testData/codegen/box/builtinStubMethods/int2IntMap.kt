// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8

// FILE: Int2IntFunction.java
public interface Int2IntFunction {
    boolean containsKey(int key);

    @Deprecated
    default boolean containsKey(Object key) {
        return false;
    }
}
// FILE: Int2IntMap.java
public interface Int2IntMap extends Int2IntFunction, java.util.Map<Integer, Integer> {
    boolean containsKey(int var1);

    @Deprecated
    default boolean containsKey(Object key) {
        return Int2IntFunction.super.containsKey(key);
    }
}

// FILE: Int2IntMapImpl.java
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class Int2IntMapImpl implements Int2IntMap {
    @Override
    public boolean containsKey(int var1) {
        return var1 == 56;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Integer get(Object key) {
        return null;
    }

    @Override
    public Integer put(Integer key, Integer value) {
        return null;
    }

    @Override
    public Integer remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<Integer> keySet() {
        return null;
    }

    @Override
    public Collection<Integer> values() {
        return null;
    }

    @Override
    public Set<Entry<Integer, Integer>> entrySet() {
        return null;
    }
}


// FILE: m.kt
fun foo(x: Int2IntMap): String {
    if (!x.containsKey(56)) return "fail 1"
    if (x.containsKey(239)) return "fail 1"
    return "OK"
}

fun box(): String {
    return foo(Int2IntMapImpl())
}
