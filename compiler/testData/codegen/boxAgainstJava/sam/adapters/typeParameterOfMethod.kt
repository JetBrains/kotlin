// FILE: WeirdComparator.java

import java.util.*;

class WeirdComparator {
    public static <T> T max(Comparator<T> comparator, T value1, T value2) {
        return comparator.compare(value1, value2) > 0 ? value1 : value2;
    }

    public static <T extends CharSequence> T max2(Comparator<T> comparator, T value1, T value2) {
        return comparator.compare(value1, value2) > 0 ? value1 : value2;
    }
}

// FILE: 1.kt

fun box(): String {
    val result = WeirdComparator.max<String>({ a, b -> a.length - b.length }, "java", "kotlin")
    if (result != "kotlin") return "Wrong: $result"

    val result2 = WeirdComparator.max2<String>({ a, b -> a.length - b.length }, "java", "kotlin")
    if (result2 != "kotlin") return "Wrong: $result"

    return "OK"
}
