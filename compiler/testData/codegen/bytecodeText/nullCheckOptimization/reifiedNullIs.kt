// IGNORE_BACKEND: JVM_IR
inline fun <reified T> isNullable() = null is T

// 1 INSTANCEOF
// 1 reifiedOperationMarker