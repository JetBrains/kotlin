// FULL_JDK

// FILE: Util.java
public class Util {
    public static String getString() { return null; }
}

// FILE: main.kt
import java.util.concurrent.ConcurrentHashMap

fun testWithMap(map: ConcurrentHashMap<Int, String>): Int {
    var string = map[1]
    if (string == null) {
        string = map.computeIfAbsent(1) { "hello" }
    }
    return string.length
}

fun testWithUtil(map: ConcurrentHashMap<Int, String>): Int {
    var string = map[1]
    if (string == null) {
        string = Util.getString()
    }
    return string.length
}

fun test(list: java.util.ArrayList<String?>) {
    val x = list.get(0)<!UNSAFE_CALL!>.<!>length
}
