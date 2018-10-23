// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface KCallable {
    @JvmDefault
    val returnType: String
}

interface KCallableImpl : KCallable {
    @JvmDefault
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