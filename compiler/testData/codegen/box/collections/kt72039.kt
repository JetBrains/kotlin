// FULL_JDK

// FILE: kt72039.kt
fun box():String {
    Test1().test()
    return "OK"
}

class Test1 : Test0()

// FILE: Test0.java
import java.util.concurrent.ConcurrentHashMap;

public class Test0 extends ConcurrentHashMap<String, String> {
    public void test() {
        new Test1().keySet();
    }
}