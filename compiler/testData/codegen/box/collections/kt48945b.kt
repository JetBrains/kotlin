// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// IGNORE_BACKEND: ANDROID
//  ^ NSME: java.util.AbstractMap.remove
// FULL_JDK

// FILE: kt48945b.kt
interface MSS : Map<String, String>

class Test : MSS, JASM<String>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = throw Exception()
}

fun box(): String {
    Test().remove(null, "")
    return "OK"
}

// FILE: JASM.java
public abstract class JASM<V> extends java.util.AbstractMap<String, V> {
    @Override
    public V get(Object key) {
        return super.get(key);
    }
}
