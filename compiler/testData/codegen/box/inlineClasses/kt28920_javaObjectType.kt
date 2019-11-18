// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun box(): String {
    val c = UInt::class.javaObjectType 
    val x = c.cast(123u)
    if (x != 123u) throw AssertionError()

    val uIntClass = UInt::class
    val cc = uIntClass.javaObjectType
    val xx = cc.cast(123u)
    if (xx != 123u) throw AssertionError()

    return "OK"
}