// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// WITH_RUNTIME

@JvmDefaultWithoutCompatibility
interface NoDefaultImpl {
    fun test() {}
}

interface WithDefaultImpl: NoDefaultImpl {

}

interface WithDefaultImplPure {
    fun test() {}
}

@JvmDefaultWithoutCompatibility
interface NoDefaultImpl2FromDefaultImpls : WithDefaultImplPure {
    fun test2() {}
}

@JvmDefaultWithoutCompatibility
class KotlinClass : NoDefaultImpl