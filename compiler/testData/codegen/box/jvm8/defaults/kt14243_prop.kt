// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Z<T> {

    val value: T

    @JvmDefault
    val z: T
        get() = value
}

open class ZImpl : Z<String> {
    override val value: String
        get() = "OK"
}

open class ZImpl2 : ZImpl() {
    override val z: String
        get() = super.z
}


fun box(): String {
    return ZImpl2().value
}
