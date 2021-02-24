// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// MODULE: lib
// FILE: A.java
import java.util.HashMap;

public class A extends HashMap<Integer, Double> {
    public double put(int x, double y) {
        return 1.0;
    }

    @Override
    public Double put(Integer key, Double value) {
        return super.put(key, value);
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val o = A()
    o.put(1, 2.0)

    return "OK"
}
