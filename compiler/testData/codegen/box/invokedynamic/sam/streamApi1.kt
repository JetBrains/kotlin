// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// FULL_JDK

// no stream api on Android
// IGNORE_BACKEND: ANDROID

import java.util.stream.Collectors

fun box(): String {
    return listOf("o", "k")
        .stream()
        .map { it.toUpperCase() }
        .collect(Collectors.joining())
}