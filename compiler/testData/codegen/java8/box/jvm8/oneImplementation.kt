// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS
interface KCallable {
    val returnType: String
}

interface KCallableImpl : KCallable {
    override val returnType: String
        get() = "OK"
}

interface KCallableImpl2 : KCallableImpl

open class DescriptorBasedProperty : KCallableImpl
open class KProperty1Impl : DescriptorBasedProperty(), KCallableImpl2
open class KMutableProperty1Impl : KProperty1Impl(), KCallable

fun box(): String {
    return KMutableProperty1Impl().returnType
}