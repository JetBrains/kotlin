// TARGET_BACKEND: JVM

// FILE: Test.java

import java.lang.*;
import java.util.*;

public class Test {
    public static class MapEntryImpl implements Map.Entry<String, String> {
        public String getKey() { return null; }
        public String getValue() { return null; }
        public String setValue(String s) { return null; }
    }
}

// FILE: main.kt

//class MyIterable : Test.IterableImpl()
//class MyIterator : Test.IteratorImpl()
class MyMapEntry : Test.MapEntryImpl()

fun box(): String {

    val b = MyMapEntry()
    b.key
    b.value
    b.setValue(null)

    return "OK"
}
