// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// WITH_STDLIB

@JvmDefaultWithoutCompatibility
interface NoDefaultImpl {
    fun test() {}
    val prop: String
        get() = "123"
}

interface WithDefaultImpl: NoDefaultImpl {

}

interface WithDefaultImplPure {
    fun test() {}
    val prop: String
        get() = "123"
}

@JvmDefaultWithoutCompatibility
interface NoDefaultImpl2FromDefaultImpls : WithDefaultImplPure {
    fun test2() {}
}

@JvmDefaultWithoutCompatibility
class KotlinClass : NoDefaultImpl