// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// JVM_DEFAULT_MODE: enable

@JvmDefaultWithoutCompatibility
interface A<T> {
    fun test(p: T) {}
}

@JvmDefaultWithoutCompatibility
class B : A<String> {}
