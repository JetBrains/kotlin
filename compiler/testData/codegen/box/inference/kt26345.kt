// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// JVM_TARGET: 1.8
// IGNORE_BACKEND: ANDROID

import java.util.stream.Collectors
import java.util.function.Function

fun box(): String {
    val m = listOf("OK", "OK")
        .stream()
        .collect(
            Collectors.groupingBy(
                Function.identity(),
                Collectors.counting()
            )
        )
    if (m["OK"] != 2L) return "fail: ${m["OK"]}"
    return "OK"
}