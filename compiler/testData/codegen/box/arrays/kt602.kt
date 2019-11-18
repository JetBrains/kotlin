// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun box() = if(arrayOfNulls<Int>(10).isArrayOf<java.lang.Integer>()) "OK" else "fail"
