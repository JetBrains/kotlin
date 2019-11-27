// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

import java.util.*;

public class J {

    private static class MyMap<K, V> extends KMap<K, V> {}

    public static String foo() {
        Map<String, Integer> collection = new MyMap<String, Integer>();
        if (!collection.containsKey("ABCDE")) return "fail 1";
        if (!collection.containsValue(1)) return "fail 2";
        return "OK";
    }
}

// FILE: test.kt

open class KMap<K, V> : Map<K, V> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsKey(key: K) = true
    override fun containsValue(value: V) = true

    override fun get(key: K): V? {
        throw UnsupportedOperationException()
    }

    override val keys: Set<K>
        get() = throw UnsupportedOperationException()
    override val values: Collection<V>
        get() = throw UnsupportedOperationException()
    override val entries: Set<Map.Entry<K, V>>
        get() = throw UnsupportedOperationException()
}

fun box() = J.foo()
