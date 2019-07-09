// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun box() = if(arrayOfNulls<Int>(10).isArrayOf<java.lang.Integer>()) "OK" else "fail"
