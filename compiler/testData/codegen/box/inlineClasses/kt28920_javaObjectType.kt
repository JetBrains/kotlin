// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun box(): String {
    val c = UInt::class.javaObjectType 
    val x = c.cast(123u)

    if (x != 123u) throw AssertionError()

    return "OK"
}