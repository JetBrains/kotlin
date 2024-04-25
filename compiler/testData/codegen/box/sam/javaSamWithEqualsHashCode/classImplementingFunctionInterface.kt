// LANGUAGE: +JavaSamConversionEqualsHashCode
// TARGET_BACKEND: JVM_IR
// FULL_JDK

class F(val v: String) : () -> Unit {
    override fun invoke() {}

    override fun equals(other: Any?): Boolean =
        other is F && other.v == v

    override fun hashCode(): Int =
        v.hashCode()
}

fun box(): String {
    val r1 = Runnable(F("abc"))
    val r2 = Runnable(F("abc"))

    if (r1 != r2)
        return "r1 != r2"
    if (r1.hashCode() != r2.hashCode())
        return "r1.hashCode() != r2.hashCode()"

    return "OK"
}
