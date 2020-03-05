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

class MyMapEntry : Test.MapEntryImpl()

fun test() {
    val b = MyMapEntry()
    val key = b.key
    val value = b.value
    b.setValue(null)
}
