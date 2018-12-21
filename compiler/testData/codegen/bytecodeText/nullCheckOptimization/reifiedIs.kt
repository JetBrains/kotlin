// IGNORE_BACKEND: JVM_IR
inline fun <reified T> Any?.isa() = this is T

// 1 INSTANCEOF
// 1 reifiedOperationMarker
