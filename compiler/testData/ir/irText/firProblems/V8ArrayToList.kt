// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FILE: Utils.java

import java.util.*;

public class Utils {
    public static List<? super Object> toList(final V8Array array) {
        List<? super Object> list = new ArrayList<Object>();
        list.add("OK");
        return list;
    }
}

// FILE: v8arrayToList.kt

class V8Array

fun box(): String {
    val array = V8Array()
    val list = Utils.toList(array) as List<String>
    return list[0]
}