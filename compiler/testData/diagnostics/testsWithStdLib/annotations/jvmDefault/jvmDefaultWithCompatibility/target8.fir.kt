// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: all

@JvmDefaultWithCompatibility
interface A<T> {
    fun test(p: T) {}
}

@JvmDefaultWithCompatibility
class B : A<String> {}