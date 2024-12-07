// LANGUAGE: +JavaSamConversionEqualsHashCode
// TARGET_BACKEND: JVM_IR
// FULL_JDK

fun box(): String {
    val lambda: () -> Unit = { }
    val r1 = Runnable(lambda)
    val r2 = Runnable(lambda)

    if (r1 != r2)
        return "r1 != r2"
    if (r1.hashCode() != r2.hashCode())
        return "r1.hashCode() != r2.hashCode()"

    return "OK"
}
