// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// JVM_TARGET: 1.8
// JVM_DEFAULT_MODE: enable

@JvmDefaultWithoutCompatibility
interface A<T> {
    fun test(p: T) {}
}

class B : A<String> {}
