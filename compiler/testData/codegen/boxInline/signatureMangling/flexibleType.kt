// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FILE: UseFlexibleType.java
import java.util.List;

public class UseFlexibleType {
    static public List<String> useList(List<String> arg) {
        return arg;
    }
}

// FILE: use.kt
inline fun callTest() = UseFlexibleType.useList(listOf("OK"))

// FILE: box.kt
fun box() = callTest()[0]
