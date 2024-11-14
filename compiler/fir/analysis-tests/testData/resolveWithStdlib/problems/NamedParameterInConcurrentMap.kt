// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

import java.util.concurrent.ConcurrentHashMap

fun main() {
    val map = ConcurrentHashMap<String, String>()
    map.put(
        key = "key",
        value = "value"
    )
}
