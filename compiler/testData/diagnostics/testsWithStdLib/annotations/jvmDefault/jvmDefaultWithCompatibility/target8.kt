// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// JVM_TARGET: 1.8
// JVM_DEFAULT_MODE: no-compatibility

@JvmDefaultWithCompatibility
interface A<T> {
    fun test(p: T) {}
}

<!JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE!>@JvmDefaultWithCompatibility<!>
class B : A<String> {}
