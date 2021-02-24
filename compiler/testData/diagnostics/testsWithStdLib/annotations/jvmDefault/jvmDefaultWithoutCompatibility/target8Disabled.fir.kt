// !JVM_TARGET: 1.8

@JvmDefaultWithoutCompatibility
interface A<T> {
    fun test(p: T) {}
}

@JvmDefaultWithoutCompatibility
class B : A<String> {}
