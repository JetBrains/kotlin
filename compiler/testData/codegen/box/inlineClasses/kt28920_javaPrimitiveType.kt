// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun box(): String {
    if (UInt::class.javaPrimitiveType != null) throw AssertionError()

    val uIntClass = UInt::class
    if (uIntClass.javaPrimitiveType != null) throw AssertionError()

    return "OK"
}