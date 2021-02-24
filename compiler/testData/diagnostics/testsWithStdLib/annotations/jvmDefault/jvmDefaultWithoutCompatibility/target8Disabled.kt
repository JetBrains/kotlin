// !JVM_TARGET: 1.8

<!JVM_DEFAULT_IN_DECLARATION!>@JvmDefaultWithoutCompatibility<!>
interface A<T> {
    fun test(p: T) {}
}

<!JVM_DEFAULT_IN_DECLARATION!>@JvmDefaultWithoutCompatibility<!>
class B : A<String> {}
