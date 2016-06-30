// FILE: AbstractSpecializedMap.java
public abstract class AbstractSpecializedMap implements java.util.Map<Integer, Double> {
    public abstract double put(int x, double y);
    public abstract double remove(int k);
    public abstract double get(int k);

    public abstract boolean containsKey(int k);
    public boolean containsKey(Object x) {
        return false;
    }

    public abstract boolean containsValue(double v);
    public boolean containsValue(Object x) {
        return false;
    }
}

// FILE: SpecializedMap.java
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SpecializedMap extends AbstractSpecializedMap {
    public double put(int x, double y) {
        return 123.0;
    }

    @Override
    public Double get(Object key) {
        return null;
    }

    @Override
    public Double put(Integer key, Double value) {
        return null;
    }

    public double remove(int k) {
        return 456.0;
    }


    public Double remove(Object ok) {
        return null;
    }


    public double get(int k) {
        return 789.0;
    }

    public boolean containsKey(int k) {
        return true;
    }

    public boolean containsValue(double v) {
        return true;
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Double> m) {
    }

    @Override
    public void clear() {

    }

    @NotNull
    @Override
    public Set<Integer> keySet() {
        return null;
    }

    @NotNull
    @Override
    public Collection<Double> values() {
        return null;
    }

    @NotNull
    @Override
    public Set<Entry<Integer, Double>> entrySet() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}

// FILE: main.kt
fun box(): String {
    val x = SpecializedMap()
    if (!x.containsKey(1)) return "fail 1"
    if (x.containsKey(null)) return "fail 2"

    if (!x.containsValue(2.0)) return "fail 3"
    if (x.containsValue(null)) return "fail 4"

    if (x.put(1, 5.0) != 123.0) return "fail 5"
    if (x.put(1, null) != null) return "fail 6"

    if (x.remove(1) != 456.0) return "fail 7"
    if (x.remove(null) != null) return "fail 8"

    if (x.get(1) != 789.0) return "fail 9"
    if (x.get(null) != null) return "fail 10"

    return "OK"
}
