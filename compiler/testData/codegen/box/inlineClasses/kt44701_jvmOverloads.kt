// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_RUNTIME

inline class Location @JvmOverloads constructor(val value: String? = "OK")

fun box(): String = Location().value!!
