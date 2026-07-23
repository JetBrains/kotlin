// TARGET_BACKEND: JVM

// FILE: JavaClass.java
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public abstract class JavaClass {
    public void testPrimitiveArray(int[] arr) {}
    public void testString(String s) {}
    public void testObject(Object o) {}
    public void testConcreteMap(HashMap<String, String> map) {}
    public void testInterfaceMap(Map<String, String> map) {}
}

// FILE: main.kt
import java.util.HashMap
import java.util.ArrayList

class KotlinSub : JavaClass() {
    override fun testPrimitiveArray(arr: IntArray) {
        super.testPrimitiveArray(arr)
    }

    override fun testString(s: String) {
        super.testString(s)
    }

    override fun testObject(o: Any) {
        super.testObject(o)
    }

    override fun testConcreteMap(map: HashMap<String, String>) {
        super.testConcreteMap(map)
    }

    override fun testInterfaceMap(map: Map<String, String>) {
        super.testInterfaceMap(map)
    }
}
