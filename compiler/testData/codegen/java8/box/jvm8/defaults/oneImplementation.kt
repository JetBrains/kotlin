// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface KCallable {
    @kotlin.annotations.JvmDefault
    val returnType: String
}

interface KCallableImpl : KCallable {
    @kotlin.annotations.JvmDefault
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