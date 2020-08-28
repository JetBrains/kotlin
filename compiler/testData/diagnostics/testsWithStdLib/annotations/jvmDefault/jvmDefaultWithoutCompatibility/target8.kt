// FIR_IDENTICAL
// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: all-compatibility

@JvmDefaultWithoutCompatibility
interface A<T> {
    fun test(p: T) {}
}

class B : A<String> {}
