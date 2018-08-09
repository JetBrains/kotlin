// IGNORE_BACKEND: JVM_IR
// FILE: WeirdComparator.java

import java.util.*;

class WeirdComparator<T> {
    public T max(Comparator<T> comparator, T value1, T value2) {
        return comparator.compare(value1, value2) > 0 ? value1 : value2;
    }
}

// FILE: 1.kt

fun box(): String {
    val wc = WeirdComparator<String>()
    val result = wc.max({ a, b -> a.length - b.length }, "java", "kotlin")
    if (result != "kotlin") return "Wrong: $result"
    return "OK"
}
