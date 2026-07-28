// LANGUAGE: +ContextParameters
// WITH_STDLIB
// TARGET_PLATFORM: JVM

@JvmInline
value class Some(val value: String)

class RegularClass {
    context(a: Some)
    fun Boolean.funWithValueClassContextParameter(param: Long) {}

    context(a: Some)
    val Boolean.propertyWithValueClassContextParameter: Int get() = 0
}
