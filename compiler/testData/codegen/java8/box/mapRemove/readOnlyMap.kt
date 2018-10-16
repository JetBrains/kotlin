// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME

// FILE: A.java
public class A {
    public static void foo(java.util.Map<String, String> x) {
        x.remove("abc", "cde");
    }
}

// FILE: main.kt

class ReadOnlyMap<K, V>(val x: K, val y: V) : Map<K, V> {
    override val entries: Set<Map.Entry<K, V>>
        get() = throw UnsupportedOperationException()
    override val keys: Set<K>
        get() = throw UnsupportedOperationException()
    override val size: Int
        get() = throw UnsupportedOperationException()
    override val values: Collection<V>
        get() = throw UnsupportedOperationException()

    override fun containsKey(key: K) = key == x

    override fun containsValue(value: V) = value == y

    override fun get(key: K): V? = if (key == x) y else null

    override fun isEmpty() = false
}

fun box(): String {
    try {
        A.foo(ReadOnlyMap("abc", "cde"))
        return "fail 1"
    } catch (e: UnsupportedOperationException) { }

    try {
        // Default Map 'remove' implenetation actually does remove iff entry exists
        A.foo(ReadOnlyMap("abc", "123"))
        return "fail 2"
    } catch (e: UnsupportedOperationException) { }

    return "OK"
}
