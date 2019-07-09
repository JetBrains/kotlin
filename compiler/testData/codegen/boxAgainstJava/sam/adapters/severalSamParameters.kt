// FILE: JavaClass.java

import java.util.*;

class JavaClass {
    public static String findMaxAndInvokeCallback(Comparator<String> comparator, String a, String b, Runnable afterRunnable) {
        int compare = comparator.compare(a, b);
        afterRunnable.run();
        return compare > 0 ? a : b;
    }
}

// FILE: 1.kt

fun box(): String {
    var v = "FAIL"
    val max = JavaClass.findMaxAndInvokeCallback({ a, b -> a.length - b.length }, "foo", "kotlin", { v = "OK" })
    if (max != "kotlin") return "Wrong max: $max"
    return v
}
