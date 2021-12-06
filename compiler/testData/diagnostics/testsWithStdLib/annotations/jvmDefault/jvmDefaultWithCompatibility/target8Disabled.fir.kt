// !JVM_TARGET: 1.8

@JvmDefaultWithCompatibility
interface A<T> {
    fun test(p: T) {}
}

@JvmDefaultWithCompatibility
class B : A<String> {}