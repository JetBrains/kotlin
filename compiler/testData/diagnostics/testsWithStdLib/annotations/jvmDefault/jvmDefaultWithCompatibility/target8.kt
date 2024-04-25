// FIR_IDENTICAL
// JVM_TARGET: 1.8
// JVM_DEFAULT_MODE: all

@JvmDefaultWithCompatibility
interface A<T> {
    fun test(p: T) {}
}

<!JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE!>@JvmDefaultWithCompatibility<!>
class B : A<String> {}
