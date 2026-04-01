// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR
// DUMP_IR
// DUMP_EXTERNAL_CLASS: MyMap
// ISSUE: KT-72345

// FILE: MyMap.java
import java.util.HashMap;

public class MyMap<V> extends HashMap<Integer, V> {

    public V get(int key) {
        throw new RuntimeException("OK");
    }
}

// FILE: main.kt
fun box(): String {
    val test2 = MyMap<String>()
    return try {
        test2.get(1)
    } catch (e: RuntimeException) {
        e.message!!
    }
}
