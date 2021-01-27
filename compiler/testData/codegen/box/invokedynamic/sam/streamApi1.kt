// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// FULL_JDK
import java.util.stream.Collectors

fun box(): String {
    return listOf("o", "k")
        .stream()
        .map { it.toUpperCase() }
        .collect(Collectors.joining())
}