// FIR_IDENTICAL
// JVM_TARGET: 1.8

<!JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION!>@JvmDefaultWithCompatibility<!>
interface A<T> {
    fun test(p: T) {}
}

<!JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION!>@JvmDefaultWithCompatibility<!>
class B : A<String> {}
