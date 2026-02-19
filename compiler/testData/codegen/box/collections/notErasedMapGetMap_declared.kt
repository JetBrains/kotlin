// TARGET_BACKEND: JVM_IR
// ISSUE: KT-72345

// FILE: MyMap.java
import java.util.HashMap;

public class MyMap<V> extends HashMap<String, V> {

    public V get(String key) {
        throw new RuntimeException("OK");
    }
}

// FILE: main.kt
fun box(): String {
    val test2 = MyMap<String>()
    return try {
        test2.get("test")
    } catch (e: RuntimeException) {
        e.message!!
    }
}
