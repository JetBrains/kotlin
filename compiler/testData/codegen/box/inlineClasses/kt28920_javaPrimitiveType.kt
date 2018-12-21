// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun box(): String {
    if (UInt::class.javaPrimitiveType != null) throw AssertionError()
    return "OK"
}