// TARGET_BACKEND: JVM

// WITH_STDLIB

fun box() = if(arrayOfNulls<Int>(10).isArrayOf<java.lang.Integer>()) "OK" else "fail"
