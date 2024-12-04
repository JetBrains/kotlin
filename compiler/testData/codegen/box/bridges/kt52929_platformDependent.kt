// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// IGNORE_BACKEND: JVM
// FILE: lib.kt

open class KotlinMap<K> : MutableMap<K, String> by mutableMapOf() {
    override fun getOrDefault(key: K, value: String): String = value
    override fun remove(key: K, value: String): Boolean = false
}

class BreakGenericSignatures : KotlinMap<String>()

// FILE: JavaMap.java

public class JavaMap extends KotlinMap<Integer> {
    public String result() { return "OK"; }

    public String getOrDefault(Object key, String value) {
        return null;
    }

    public boolean remove(Integer key, String value) {
        return false;
    }
}

// FILE: main.kt

fun box(): String = JavaMap().result()