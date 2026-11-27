// FILE: 1.kt

inline fun sortedJoin(vararg values: String): String {
    J.mutate(*values)
    return java.util.Arrays.asList(*values).joinToString(",")
}

inline fun joinedTwice(vararg values: String): String =
    java.util.Arrays.asList(*values).joinToString("") + "|" +
            java.util.Arrays.asList(*values).joinToString("")

inline fun valueCount(vararg values: String): Int = java.util.Arrays.asList(*values).size

var counter = 0

fun nextValue(): String = "v${++counter}"

inline fun joinedValues(vararg values: String): String = java.util.Arrays.asList(*values).joinToString(",")

inline fun joinedOnce(vararg values: String): String = java.util.Arrays.asList(*values).joinToString(",")

// FILE: J.java

public class J {
    public static void mutate(String... values) {
        java.util.Arrays.sort(values);
    }
}

// FILE: 2.kt

fun box(): String {
    val sorted = sortedJoin("b", "a")
    if (sorted != "b,a") return "Fail sortedJoin: $sorted"

    val joined = joinedTwice("X", "Y")
    if (joined != "XY|XY") return "Fail joinedTwice: $joined"

    if (valueCount() != 0) return "Fail valueCount: ${valueCount()}"

    counter = 0
    val values = joinedValues(nextValue(), nextValue())
    if (values != "v1,v2" || counter != 2) return "Fail side effects: $values, counter=$counter"

    val source = arrayOf("b", "a")
    val joinedResult = joinedOnce(*source)
    if (joinedResult != "b,a") return "Fail joinedOnce: $joinedResult"
    if (source[0] != "b" || source[1] != "a") return "Fail source modified: ${source.toList()}"

    return "OK"
}
