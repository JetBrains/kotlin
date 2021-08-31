// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
// FILE: UseRawType.java
import java.util.List;

public class UseRawType {
    static public List useList(List arg) {
        return arg;
    }
}

// FILE: use.kt
inline fun callRawType() = UseRawType.useList(listOf("OK")) as List<String>

// FILE: box.kt
fun box() = callRawType()[0]