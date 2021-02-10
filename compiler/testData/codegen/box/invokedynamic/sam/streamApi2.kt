// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// FULL_JDK

// no stream api on Android
// IGNORE_BACKEND: ANDROID

import java.util.stream.*

fun box(): String {
    val xs = IntStream.of(1, 1, 4, 5, 1, 4)
        .map { a: Int -> a + 1 }
        .map { a: Int -> a - 1 }
        .filter { a: Int -> a > 3 }
        .flatMap { a: Int -> IntStream.of(a, a, a) }
        .boxed()
        .collect(Collectors.toList())

    if (xs != listOf(4, 4, 4, 5, 5, 5, 4, 4, 4))
        return "Failed: xs=$xs"

    return "OK"
}